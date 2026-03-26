package com.shop.ecommerce.service;

import com.shop.ecommerce.model.Product;
import com.shop.ecommerce.model.User;
import com.shop.ecommerce.model.UserSegment;
import com.shop.ecommerce.repository.ProductRepository;
import com.shop.ecommerce.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.data.domain.Sort;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for running ML algorithms (Phase 1, 2, 3) and updating the database
 */
@Service
public class MLService {

    private static final Logger logger = LoggerFactory.getLogger(MLService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Value("${ml.service.url:http://localhost:5000}")
    private String mlServiceUrl;

    @Value("${ml.service.auto-start:true}")
    private boolean mlServiceAutoStart;

    @Value("${ml.categorize.fallback-to-python:false}")
    private boolean mlCategorizeFallbackToPython;

    private final RestTemplate restTemplate = new RestTemplate();

    private final String projectRoot;
    private final Path datasetsPath;
    private final Path resultsPath;

    public MLService() {
        // Get project root - try multiple approaches
        String projectRootValue = System.getProperty("user.dir", ".");
        try {
            String userDir = System.getProperty("user.dir");
            
            // If running from backend directory, go up one level
            if (userDir != null && (userDir.endsWith("backend") || userDir.endsWith("backend\\") || userDir.endsWith("backend/"))) {
                Path parentPath = Paths.get(userDir).getParent();
                projectRootValue = parentPath != null ? parentPath.toString() : userDir;
            } else {
                // Try to find ML-eCommers-GitHub directory
                Path currentPath = Paths.get(userDir != null ? userDir : ".");
                while (currentPath != null) {
                    Path fileName = currentPath.getFileName();
                    if (fileName != null && fileName.toString().startsWith("ML-eCommers-GitHub")) {
                        projectRootValue = currentPath.toString();
                        break;
                    }
                    Path parent = currentPath.getParent();
                    // Stop if we've reached the root (parent is null or same as current)
                    if (parent == null || parent.equals(currentPath)) {
                        // Fallback: assume we're in project root
                        projectRootValue = userDir != null ? userDir : ".";
                        break;
                    }
                    currentPath = parent;
                }
            }
        } catch (Exception e) {
            logger.error("Error initializing MLService paths", e);
            // Set defaults to prevent startup failure
            projectRootValue = System.getProperty("user.dir", ".");
        }
        
        // If project root has no src/phase1 (e.g. workspace root with nested repo), use nested folder (single path check, no directory listing)
        Path candidateRoot = Paths.get(projectRootValue);
        if (!Files.isDirectory(candidateRoot.resolve("src").resolve("phase1"))) {
            Path nested = candidateRoot.resolve("ML-eCommers-GitHub-9.2.26");
            if (Files.isDirectory(nested) && Files.isDirectory(nested.resolve("src").resolve("phase1"))) {
                projectRootValue = nested.toString();
                logger.info("ML Service: using nested project root: {}", projectRootValue);
            } else {
                Path fromList = resolveProjectRootWithPhase1(candidateRoot);
                if (fromList != null) projectRootValue = fromList.toString();
            }
        }
        
        // Assign to final fields
        this.projectRoot = projectRootValue;
        this.datasetsPath = Paths.get(projectRoot, "datasets");
        this.resultsPath = datasetsPath.resolve("results");
        
        logger.info("ML Service initialized - Project root: {}", this.projectRoot);
        logger.info("Datasets path: {}", this.datasetsPath);
        logger.info("Results path: {}", this.resultsPath);
    }
    
    /** If candidate has no src/phase1, look for a direct child directory that contains src/phase1 (nested repo). */
    private static Path resolveProjectRootWithPhase1(Path candidate) {
        try {
            if (!Files.isDirectory(candidate)) return null;
            try (var stream = Files.list(candidate)) {
                List<Path> children = new ArrayList<>();
                stream.forEach(children::add);
                for (Path child : children) {
                    if (Files.isDirectory(child) && Files.isDirectory(child.resolve("src").resolve("phase1"))) {
                        return child;
                    }
                }
            }
        } catch (Exception ignored) { }
        return null;
    }

    /**
     * When the app is ready, if ml.service.auto-start is true and ML service is not reachable,
     * start the Flask ml_service (Python) in the background so product categorization is fast without Docker.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startMlServiceInBackgroundIfNeeded() {
        if (!mlServiceAutoStart) {
            logger.debug("ML service auto-start disabled");
            return;
        }
        try {
            restTemplate.getForEntity(mlServiceUrl + "/health", String.class);
            logger.info("ML service already running at {}", mlServiceUrl);
            return;
        } catch (Exception e) {
            logger.debug("ML service not reachable, starting in background: {}", e.getMessage());
        }
        Path appPy = Paths.get(projectRoot).resolve("backend").resolve("ml_service").resolve("app.py");
        if (!Files.isRegularFile(appPy)) {
            logger.warn("ML service script not found at {}, skipping auto-start", appPy.toAbsolutePath());
            return;
        }
        try {
            Path logDir = Paths.get(projectRoot);
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(logDir.toFile());
            pb.redirectOutput(Redirect.appendTo(logDir.resolve("ml_service_out.log").toFile()));
            pb.redirectError(Redirect.appendTo(logDir.resolve("ml_service_err.log").toFile()));
            String scriptPath = appPy.toString();
            boolean isWin = System.getProperty("os.name", "").toLowerCase().startsWith("win");
            if (isWin) {
                try {
                    pb.command("python", scriptPath);
                    Process p = pb.start();
                    logger.info("ML service started in background at {} (PID {}). Logs: ml_service_out.log / ml_service_err.log", mlServiceUrl, p.pid());
                    return;
                } catch (IOException e1) {
                    pb.command("py", "-3", scriptPath);
                }
            } else {
                pb.command("python3", scriptPath);
            }
            Process p = pb.start();
            logger.info("ML service started in background at {} (PID {}). Logs: ml_service_out.log / ml_service_err.log", mlServiceUrl, p.pid());
        } catch (Exception e) {
            logger.warn("Could not start ML service in background: {}", e.getMessage());
        }
    }
    /**
     * Runs Phase 1: User and Product Categorization
     * 
     * Stages:
     * 1. Execute Python scripts for user and product categorization
     * 2. Read results from CSV files
     * 3. Update users in database with ML categories
     * 4. Update products in database with ML categories
     * 
     * @return Map with execution results and statistics
     */
    public Map<String, Object> runPhase1() {
        logger.info("=".repeat(80));
        logger.info("Starting Phase 1: User and Product Categorization");
        logger.info("=".repeat(80));

        Map<String, Object> results = new HashMap<>();
        Map<String, Object> stages = new HashMap<>();

        try {
            // Stage 0: Export current products from DB to CSV (single file, overwritten each run)
            logger.info("\n[STAGE 0] Exporting current products from DB for Phase 1...");
            int exported = exportProductsForPhase1();
            logger.info("  Exported {} products to current_products_for_phase1.csv (overwritten)", exported);

            // Stage 1: Run Python scripts
            logger.info("\n[STAGE 1] Executing Python ML scripts...");
            stages.put("stage1_execute_scripts", executePhase1Scripts());
            
            // Stage 2: Read results from CSV
            logger.info("\n[STAGE 2] Reading results from CSV files...");
            Map<String, Object> readResults = readPhase1Results();
            stages.put("stage2_read_results", readResults);
            
            // Stage 3: Update users in database
            logger.info("\n[STAGE 3] Updating users in PostgreSQL database...");
            Map<String, Object> userUpdateResults = updateUsersInDatabase(readResults);
            stages.put("stage3_update_users", userUpdateResults);
            
            // Stage 4: Update products in database
            logger.info("\n[STAGE 4] Updating products in PostgreSQL database...");
            Map<String, Object> productUpdateResults = updateProductsInDatabase(readResults);
            stages.put("stage4_update_products", productUpdateResults);

            results.put("success", true);
            results.put("stages", stages);
            results.put("summary", createSummary(userUpdateResults, productUpdateResults));

            logger.info("\n" + "=".repeat(80));
            logger.info("Phase 1 completed successfully!");
            logger.info("=".repeat(80));

        } catch (Exception e) {
            logger.error("Error running Phase 1", e);
            results.put("success", false);
            results.put("error", e.getMessage());
            results.put("stages", stages);
        }

        return results;
    }

    /**
     * One-time sync: apply product categories from datasets/results/phase1/products_with_categories.csv
     * to the database. Does not run Phase 1 Python scripts or export from DB.
     * Use this when you have the correct categories in the CSV and want to fix the DB once;
     * after that, single-product ML (on add/update) uses the demo catalog and does not overwrite.
     *
     * @param byRowOrder if true, CSV row 2 → DB product id 1, row 3 → id 2, etc. (ignores CSV id column).
     *                   Use when CSV ids don't match seed/DB (e.g. different dataset). If false, uses seed order mapping.
     * @return Map with success, message, and apply_csv stage (updated, not_found, errors)
     */
    public Map<String, Object> syncProductCategoriesFromCsv(boolean byRowOrder) {
        Map<String, Object> result = new HashMap<>();
        try {
            Path csvPath = resolvePhase1ProductsCsvPath();
            if (csvPath == null || !Files.exists(csvPath)) {
                result.put("success", false);
                result.put("message", "products_with_categories.csv not found. Ensure datasets/results/phase1/products_with_categories.csv exists.");
                return result;
            }
            if (byRowOrder) {
                List<Map<String, String>> rows = readProductCategoriesByRowOrder(csvPath);
                if (rows.isEmpty()) {
                    result.put("success", false);
                    result.put("message", "No rows in CSV or could not parse predicted_main_category / predicted_sub_category.");
                    return result;
                }
                Map<String, Object> updateResults = updateProductsInDatabaseByRowOrder(rows);
                result.put("success", true);
                result.put("message", "Product categories applied by row order (CSV line 2 → DB id 1, line 3 → id 2, ...).");
                result.put("stages", Map.of("apply_csv", updateResults));
                result.put("summary", Map.of(
                        "products_updated", updateResults.get("updated"),
                        "products_not_found", updateResults.get("not_found"),
                        "errors", updateResults.get("errors")
                ));
                logger.info("Sync product categories from CSV (byRowOrder) complete: {}", updateResults);
                return result;
            }
            Map<String, Object> readResults = readPhase1Results();
            if (!Boolean.TRUE.equals(readResults.get("success"))) {
                result.put("success", false);
                result.put("error", readResults.get("error"));
                result.put("message", "Could not read products_with_categories.csv.");
                return result;
            }
            @SuppressWarnings("unchecked")
            Map<Integer, Map<String, String>> productCategories = (Map<Integer, Map<String, String>>) readResults.get("product_categories");
            if (productCategories == null || productCategories.isEmpty()) {
                result.put("success", false);
                result.put("message", "No product_categories in CSV or file not found.");
                return result;
            }
            Map<String, Object> updateResults = updateProductsInDatabase(readResults);
            result.put("success", true);
            result.put("message", "Product categories applied from products_with_categories.csv (seed order mapping).");
            result.put("stages", Map.of("apply_csv", updateResults));
            result.put("summary", Map.of(
                    "products_updated", updateResults.get("updated"),
                    "products_not_found", updateResults.get("not_found"),
                    "errors", updateResults.get("errors")
            ));
            logger.info("Sync product categories from CSV complete: {}", updateResults);
            return result;
        } catch (Exception e) {
            logger.error("Error syncing product categories from CSV", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /** Overload: sync using seed/id mapping (original behavior). */
    public Map<String, Object> syncProductCategoriesFromCsv() {
        return syncProductCategoriesFromCsv(false);
    }

    /**
     * Apply product categories from the seed file (seed/products.csv) to the database by row order.
     * DB product id 1 = seed row 1, id 2 = seed row 2, etc. This makes the DB match "how it's supposed to get it from the seed".
     *
     * @return Map with success, message, and summary (updated, not_found, errors)
     */
    public Map<String, Object> syncCategoriesFromSeed() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, String>> rows = readSeedCategoriesByRowOrder();
            if (rows.isEmpty()) {
                result.put("success", false);
                result.put("message", "Could not read seed/products.csv or no rows with main_category found.");
                return result;
            }
            Map<String, Object> updateResults = updateProductsInDatabaseByRowOrder(rows);
            result.put("success", true);
            result.put("message", "Product categories applied from seed/products.csv (DB id 1 = seed row 1, id 2 = row 2, ...).");
            result.put("stages", Map.of("apply_seed", updateResults));
            result.put("summary", Map.of(
                    "products_updated", updateResults.get("updated"),
                    "products_not_found", updateResults.get("not_found"),
                    "errors", updateResults.get("errors")
            ));
            logger.info("Sync categories from seed complete: {}", updateResults);
            return result;
        } catch (Exception e) {
            logger.error("Error syncing categories from seed", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /** Read seed/products.csv (classpath) into list of { main_category, sub_category } in row order. */
    private List<Map<String, String>> readSeedCategoriesByRowOrder() throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource("seed/products.csv");
        if (!resource.exists()) return rows;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) return rows;
            String[] headers = parseCSVLine(header);
            int mainCategoryIndex = -1;
            int subCategoryIndex = -1;
            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].trim().replace("\"", "").replace("\uFEFF", "").toLowerCase();
                if (h.equals("main_category")) mainCategoryIndex = i;
                if (h.equals("sub_category")) subCategoryIndex = i;
            }
            if (mainCategoryIndex < 0) return rows;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = parseCSVLine(line);
                Map<String, String> map = new HashMap<>();
                if (mainCategoryIndex < parts.length)
                    map.put("main_category", parts[mainCategoryIndex].trim().replace("\"", ""));
                if (subCategoryIndex >= 0 && subCategoryIndex < parts.length)
                    map.put("sub_category", parts[subCategoryIndex].trim().replace("\"", ""));
                rows.add(map);
            }
        }
        return rows;
    }

    /** Resolves path to datasets/results/phase1/products_with_categories.csv (same logic as readPhase1Results). */
    private Path resolvePhase1ProductsCsvPath() {
        Path phase1ResultsPath = resultsPath.resolve("phase1");
        Path productsPath = phase1ResultsPath.resolve("products_with_categories.csv");
        if (Files.exists(productsPath)) return productsPath;
        Path nested = Paths.get(projectRoot).resolve("ML-eCommers-GitHub-9.2.26");
        Path nestedPhase1 = nested.resolve("datasets").resolve("results").resolve("phase1");
        if (Files.exists(nestedPhase1.resolve("products_with_categories.csv")))
            return nestedPhase1.resolve("products_with_categories.csv");
        Path fromList = resolveProjectRootWithPhase1(Paths.get(projectRoot));
        if (fromList != null) {
            Path p = fromList.resolve("datasets").resolve("results").resolve("phase1").resolve("products_with_categories.csv");
            if (Files.exists(p)) return p;
        }
        return productsPath;
    }

    /** Read CSV into list of category maps in row order (line 2 = index 0, line 3 = index 1, ...). */
    private List<Map<String, String>> readProductCategoriesByRowOrder(Path csvPath) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null) return rows;
            String[] headers = parseCSVLine(header);
            int mainCategoryIndex = -1;
            int subCategoryIndex = -1;
            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].trim().replace("\"", "").toLowerCase();
                if (h.equals("predicted_main_category")) mainCategoryIndex = i;
                if (h.equals("predicted_sub_category")) subCategoryIndex = i;
            }
            if (mainCategoryIndex < 0) return rows;
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = parseCSVLine(line);
                Map<String, String> map = new HashMap<>();
                if (mainCategoryIndex < parts.length)
                    map.put("main_category", parts[mainCategoryIndex].trim().replace("\"", ""));
                if (subCategoryIndex >= 0 && subCategoryIndex < parts.length)
                    map.put("sub_category", parts[subCategoryIndex].trim().replace("\"", ""));
                rows.add(map);
            }
        }
        return rows;
    }

    /** Update DB products by row order: product id 1 = rows.get(0), id 2 = rows.get(1), ... */
    private Map<String, Object> updateProductsInDatabaseByRowOrder(List<Map<String, String>> rows) {
        Map<String, Object> updateResults = new HashMap<>();
        int updated = 0;
        int notFound = 0;
        int errors = 0;
        List<Product> products = productRepository.findAll(Sort.by("id"));
        int n = Math.min(rows.size(), products.size());
        logger.info("  Updating products by row order: {} rows from CSV, {} products in DB, applying first {}", rows.size(), products.size(), n);
        for (int i = 0; i < n; i++) {
            Product product = products.get(i);
            Map<String, String> categories = rows.get(i);
            try {
                String mainCategory = categories.get("main_category");
                String subCategory = categories.get("sub_category");
                if (mainCategory != null && !mainCategory.isEmpty()) {
                    product.setCategory(mainCategory);
                    product.setMlCategory(mainCategory);
                }
                if (subCategory != null && !subCategory.isEmpty())
                    product.setSubCategory(subCategory);
                product.setUpdatedAt(LocalDateTime.now());
                productRepository.save(product);
                updated++;
                if (updated % 500 == 0) logger.info("  Updated {} products so far...", updated);
            } catch (Exception e) {
                errors++;
                logger.error("  Error updating product id {}: {}", product.getId(), e.getMessage());
            }
        }
        if (products.size() > n) notFound = products.size() - n;
        updateResults.put("success", true);
        updateResults.put("updated", updated);
        updateResults.put("not_found", notFound);
        updateResults.put("errors", errors);
        updateResults.put("total_processed", n);
        return updateResults;
    }

    /**
     * Exports current products from DB to datasets/raw/current_products_for_phase1.csv.
     * Overwrites the file each run - single file, no accumulation.
     * Phase 1 reads from this file when it exists (dynamic learning on live catalog).
     * If DB is empty, file is not written; Phase 1 falls back to seed/products.csv.
     *
     * @return Number of products exported, or 0 if none
     */
    private int exportProductsForPhase1() {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            logger.info("  No products in DB - Phase 1 will use seed/fallback");
            return 0;
        }
        Path projectRootPath = resolveProjectRootForPhase1();
        Path rawDir = projectRootPath.resolve("datasets").resolve("raw");
        Path csvPath = rawDir.resolve("current_products_for_phase1.csv");
        try {
            Files.createDirectories(rawDir);
            try (BufferedWriter w = Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                w.write("id,product_name,description,price,main_category,sub_category\n");
                for (Product p : products) {
                    String name = escapeCsv(p.getProductName());
                    String desc = escapeCsv(p.getDescription() != null ? p.getDescription() : "");
                    String main = escapeCsv(p.getCategory() != null && !p.getCategory().isBlank() ? p.getCategory() : p.getMlCategory() != null ? p.getMlCategory() : "Unknown");
                    String sub = escapeCsv(p.getSubCategory() != null && !p.getSubCategory().isBlank() ? p.getSubCategory() : "Unknown");
                    w.write(String.format("%d,%s,%s,%.2f,%s,%s\n",
                            p.getId(), name, desc, p.getPrice(), main, sub));
                }
            }
            return products.size();
        } catch (IOException e) {
            logger.warn("  Could not export products for Phase 1: {} - Phase 1 will use seed/fallback", e.getMessage());
            return 0;
        }
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private Path resolveProjectRootForPhase1() {
        Path projectRootPath = Paths.get(projectRoot);
        if (!Files.isDirectory(projectRootPath.resolve("src").resolve("phase1"))) {
            Path nested = projectRootPath.resolve("ML-eCommers-GitHub-9.2.26");
            if (Files.isDirectory(nested) && Files.isDirectory(nested.resolve("src").resolve("phase1"))) {
                return nested;
            }
            Path fromList = resolveProjectRootWithPhase1(projectRootPath);
            if (fromList != null) return fromList;
        }
        return projectRootPath;
    }

    /**
     * Executes Python scripts for Phase 1
     * Uses run_all_phases.py which runs both product and user categorization
     */
    private Map<String, Object> executePhase1Scripts() {
        Map<String, Object> executionResults = new HashMap<>();
        
        try {
            // Get the project root; if src/phase1 is not here (nested repo), use known nested folder (no directory listing)
            Path projectRootPath = Paths.get(projectRoot);
            if (!Files.isDirectory(projectRootPath.resolve("src").resolve("phase1"))) {
                Path nested = projectRootPath.resolve("ML-eCommers-GitHub-9.2.26");
                if (Files.isDirectory(nested.resolve("src").resolve("phase1"))) {
                    projectRootPath = nested;
                    logger.info("  Using nested project root for Phase 1: {}", projectRootPath);
                } else {
                    Path fromList = resolveProjectRootWithPhase1(projectRootPath);
                    if (fromList != null) {
                        projectRootPath = fromList;
                        logger.info("  Using nested project root for Phase 1: {}", projectRootPath);
                    }
                }
            }
            Path phase1Script = projectRootPath.resolve("run_phase1.py");
            Path runAllPhasesScript = projectRootPath.resolve("run_all_phases.py");
            
            logger.info("  Project root: {}", projectRootPath);
            
            // Verify paths exist
            if (!Files.exists(projectRootPath)) {
                throw new IOException("Project root not found: " + projectRootPath);
            }
            
            // Prefer run_all_phases.py with --phase1 flag (root or scripts/), then run_phase1.py, then fall back to individual scripts
            Path scriptToRun = null;
            String scriptName = null;
            List<String> commandArgs = new ArrayList<>();
            Path runAllInScripts = projectRootPath.resolve("scripts").resolve("run_all_phases.py");
            
            if (Files.exists(runAllPhasesScript)) {
                scriptToRun = runAllPhasesScript;
                scriptName = "run_all_phases.py";
                commandArgs.add("python");
                commandArgs.add(scriptToRun.toString());
                commandArgs.add("--phase1");  // Only run Phase 1
                logger.info("  Found run_all_phases.py - running Phase 1 (product and user categorization)...");
            } else if (Files.exists(runAllInScripts)) {
                scriptToRun = runAllInScripts;
                scriptName = "scripts/run_all_phases.py";
                commandArgs.add("python");
                commandArgs.add(scriptToRun.toString());
                commandArgs.add("--phase1");
                logger.info("  Found scripts/run_all_phases.py - running Phase 1 (product and user categorization)...");
            } else if (Files.exists(phase1Script)) {
                scriptToRun = phase1Script;
                scriptName = "run_phase1.py";
                commandArgs.add("python");
                commandArgs.add(scriptToRun.toString());
                logger.info("  Found run_phase1.py - running both product and user categorization together...");
            }
            
            if (scriptToRun != null) {
                // Run the combined script
                ProcessBuilder processBuilder = new ProcessBuilder(commandArgs);
                processBuilder.directory(projectRootPath.toFile());
                Process process = processBuilder.start();
                
                // Read output
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        logger.info("  OUTPUT: {}", line);
                    }
                }
                
                // Also read error stream
                StringBuilder errorOutput = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new java.io.InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                        logger.warn("  ERROR: {}", line);
                    }
                }
                
                int exitCode = process.waitFor();
                executionResults.put("combined_script", Map.of(
                    "script", scriptName,
                    "exit_code", exitCode,
                    "success", exitCode == 0,
                    "output", output.toString(),
                    "error_output", errorOutput.toString()
                ));
                executionResults.put("success", exitCode == 0);
                
            } else {
                // Fall back to running individual scripts
                logger.info("  run_all_phases.py not found, running individual scripts...");
                Path pythonScriptPath = projectRootPath.resolve("src").resolve("phase1");
                
                if (!Files.exists(pythonScriptPath)) {
                    throw new IOException("Python scripts path not found: " + pythonScriptPath);
                }

                // Run product categorization
                logger.info("  Running product categorization...");
                ProcessBuilder productProcess = new ProcessBuilder(
                    "python",
                    pythonScriptPath.resolve("product_categorization.py").toString()
                );
                productProcess.directory(projectRootPath.toFile());
                Process prodProcess = productProcess.start();
                
                StringBuilder productOutput = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new java.io.InputStreamReader(prodProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        productOutput.append(line).append("\n");
                        logger.info("  PRODUCT: {}", line);
                    }
                }
                
                int productExitCode = prodProcess.waitFor();
                executionResults.put("product_categorization", Map.of(
                    "exit_code", productExitCode,
                    "success", productExitCode == 0,
                    "output", productOutput.toString()
                ));

                // Run user categorization
                logger.info("  Running user categorization...");
                ProcessBuilder userProcess = new ProcessBuilder(
                    "python",
                    pythonScriptPath.resolve("user_categorization.py").toString()
                );
                userProcess.directory(projectRootPath.toFile());
                Process usrProcess = userProcess.start();
                
                StringBuilder userOutput = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new java.io.InputStreamReader(usrProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        userOutput.append(line).append("\n");
                        logger.info("  USER: {}", line);
                    }
                }
                
                int userExitCode = usrProcess.waitFor();
                executionResults.put("user_categorization", Map.of(
                    "exit_code", userExitCode,
                    "success", userExitCode == 0,
                    "output", userOutput.toString()
                ));

                executionResults.put("success", productExitCode == 0 && userExitCode == 0);
            }

        } catch (Exception e) {
            logger.error("Error executing Phase 1 scripts", e);
            executionResults.put("success", false);
            executionResults.put("error", e.getMessage());
        }

        return executionResults;
    }

    /**
     * Reads Phase 1 results from CSV files
     */
    private Map<String, Object> readPhase1Results() {
        Map<String, Object> readResults = new HashMap<>();
        
        try {
            Path phase1ResultsPath = resultsPath.resolve("phase1");
            // If CSVs are in nested repo (e.g. workspace root vs inner project), use nested path
            Path usersWithClustersPath = phase1ResultsPath.resolve("users_with_clusters.csv");
            if (!Files.exists(usersWithClustersPath)) {
                Path nested = Paths.get(projectRoot).resolve("ML-eCommers-GitHub-9.2.26");
                Path nestedPhase1 = nested.resolve("datasets").resolve("results").resolve("phase1");
                if (Files.exists(nestedPhase1.resolve("users_with_clusters.csv"))) {
                    phase1ResultsPath = nestedPhase1;
                    usersWithClustersPath = phase1ResultsPath.resolve("users_with_clusters.csv");
                    logger.info("  Reading Phase 1 results from nested path: {}", phase1ResultsPath);
                } else {
                    Path fromList = resolveProjectRootWithPhase1(Paths.get(projectRoot));
                    if (fromList != null) {
                        phase1ResultsPath = fromList.resolve("datasets").resolve("results").resolve("phase1");
                        usersWithClustersPath = phase1ResultsPath.resolve("users_with_clusters.csv");
                    }
                }
            }
            
            // Read user categorization results
            if (Files.exists(usersWithClustersPath)) {
                logger.info("  Reading user categorization results from: {}", usersWithClustersPath);
                Map<Integer, String> userCategories = readUserCategories(usersWithClustersPath);
                readResults.put("user_categories", userCategories);
                readResults.put("user_count", userCategories.size());
                logger.info("  Loaded {} user categories", userCategories.size());
            } else {
                logger.warn("  User categorization results not found: {}", usersWithClustersPath);
            }

            // Read product categorization results
            Path productsWithCategoriesPath = phase1ResultsPath.resolve("products_with_categories.csv");
            if (Files.exists(productsWithCategoriesPath)) {
                logger.info("  Reading product categorization results from: {}", productsWithCategoriesPath);
                Map<Integer, Map<String, String>> productCategories = readProductCategories(productsWithCategoriesPath);
                readResults.put("product_categories", productCategories);
                readResults.put("product_count", productCategories.size());
                logger.info("  Loaded {} product categories", productCategories.size());
            } else {
                logger.warn("  Product categorization results not found: {}", productsWithCategoriesPath);
            }

            readResults.put("success", true);

        } catch (Exception e) {
            logger.error("Error reading Phase 1 results", e);
            readResults.put("success", false);
            readResults.put("error", e.getMessage());
        }

        return readResults;
    }

    /**
     * Reads user categories from CSV file
     * CSV format: user_id,...,category (last column)
     */
    private Map<Integer, String> readUserCategories(Path csvPath) throws IOException {
        Map<Integer, String> userCategories = new HashMap<>();
        
        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            String header = reader.readLine();
            if (header == null) {
                return userCategories;
            }

            // Find column indices
            String[] headers = parseCSVLine(header);
            int userIdIndex = -1;
            int categoryIndex = -1;

            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].trim().replace("\"", "").toLowerCase();
                if (h.equals("user_id")) userIdIndex = i;
                if (h.equals("category")) categoryIndex = i;
            }

            if (userIdIndex == -1 || categoryIndex == -1) {
                logger.error("  Could not find required columns in user CSV. Found: {}", header);
                return userCategories;
            }

            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                try {
                    String[] parts = parseCSVLine(line);
                    if (parts.length > Math.max(userIdIndex, categoryIndex)) {
                        int userId = Integer.parseInt(parts[userIdIndex].trim().replace("\"", ""));
                        String category = parts[categoryIndex].trim().replace("\"", "");
                        if (!category.isEmpty() && !category.equals("category")) {
                            userCategories.put(userId, category);
                        }
                    }
                } catch (Exception e) {
                    if (lineNum <= 10) {
                        logger.warn("  Skipping invalid line {}: {}", lineNum, e.getMessage());
                    }
                }
            }
        }

        return userCategories;
    }

    /**
     * Simple CSV line parser that handles quoted fields
     */
    private String[] parseCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString()); // Add last field

        return fields.toArray(new String[0]);
    }

    /**
     * Reads product categories from CSV file
     * CSV format: id,...,predicted_main_category,predicted_sub_category
     * Note: The "id" in this CSV is the original dataset id (e.g. 278, 3118). DB product ids are 1,2,3... (insertion order).
     * Use loadSeedProductIdsOrder() to map DB id -> CSV id when applying to the database.
     */
    private Map<Integer, Map<String, String>> readProductCategories(Path csvPath) throws IOException {
        Map<Integer, Map<String, String>> productCategories = new HashMap<>();
        
        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            String header = reader.readLine();
            if (header == null) {
                return productCategories;
            }

            String[] headers = parseCSVLine(header);
            int idIndex = -1;
            int mainCategoryIndex = -1;
            int subCategoryIndex = -1;
            int productNameIndex = -1;

            // Find column indices
            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].trim().replace("\"", "").toLowerCase();
                if (h.equals("id")) idIndex = i;
                if (h.equals("predicted_main_category")) mainCategoryIndex = i;
                if (h.equals("predicted_sub_category")) subCategoryIndex = i;
                if (h.equals("product_name")) productNameIndex = i;
            }

            if (idIndex == -1) {
                logger.error("  Could not find 'id' column in product CSV. Found: {}", header);
                return productCategories;
            }

            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                try {
                    String[] parts = parseCSVLine(line);
                    if (parts.length > idIndex) {
                        int productId = Integer.parseInt(parts[idIndex].trim().replace("\"", ""));
                        Map<String, String> categories = new HashMap<>();
                        
                        if (mainCategoryIndex != -1 && parts.length > mainCategoryIndex) {
                            String mainCat = parts[mainCategoryIndex].trim().replace("\"", "");
                            if (!mainCat.isEmpty()) {
                                categories.put("main_category", mainCat);
                            }
                        }
                        if (subCategoryIndex != -1 && parts.length > subCategoryIndex) {
                            String subCat = parts[subCategoryIndex].trim().replace("\"", "");
                            if (!subCat.isEmpty()) {
                                categories.put("sub_category", subCat);
                            }
                        }
                        if (productNameIndex != -1 && parts.length > productNameIndex) {
                            String name = parts[productNameIndex].trim().replace("\"", "");
                            if (!name.isEmpty()) {
                                categories.put("product_name", name);
                            }
                        }
                        
                        if (!categories.isEmpty()) {
                            productCategories.put(productId, categories);
                        }
                    }
                } catch (Exception e) {
                    if (lineNum <= 10) {
                        logger.warn("  Skipping invalid line {}: {}", lineNum, e.getMessage());
                    }
                }
            }
        }

        return productCategories;
    }

    /**
     * Updates users in PostgreSQL database with ML categories
     */
    private Map<String, Object> updateUsersInDatabase(Map<String, Object> readResults) {
        Map<String, Object> updateResults = new HashMap<>();
        int updated = 0;
        int notFound = 0;
        int errors = 0;

        @SuppressWarnings("unchecked")
        Map<Integer, String> userCategories = (Map<Integer, String>) readResults.get("user_categories");

        if (userCategories == null || userCategories.isEmpty()) {
            logger.warn("  No user categories to update");
            updateResults.put("success", false);
            updateResults.put("message", "No user categories found");
            return updateResults;
        }

        logger.info("  Updating {} users in database...", userCategories.size());

        for (Map.Entry<Integer, String> entry : userCategories.entrySet()) {
            try {
                Integer userId = entry.getKey();
                String category = entry.getValue();

                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    user.setMlCategory(category);
                    user.setSegment(UserSegment.CLASSIFIED);
                    user.setLastClassifiedAt(LocalDateTime.now());
                    userRepository.save(user);
                    updated++;
                    
                    if (updated % 100 == 0) {
                        logger.info("  Updated {} users so far...", updated);
                    }
                } else {
                    notFound++;
                    if (notFound <= 10) {
                        logger.debug("  User ID {} not found in database", userId);
                    }
                }
            } catch (Exception e) {
                errors++;
                logger.error("  Error updating user ID {}: {}", entry.getKey(), e.getMessage());
            }
        }

        updateResults.put("success", true);
        updateResults.put("updated", updated);
        updateResults.put("not_found", notFound);
        updateResults.put("errors", errors);
        updateResults.put("total_processed", userCategories.size());

        logger.info("  User update complete: {} updated, {} not found, {} errors", 
                    updated, notFound, errors);

        return updateResults;
    }

    /**
     * Loads the ordered list of product IDs from the seed CSV (same order as DB insert).
     * DB product id 1 = first data row's id, id 2 = second row's id, etc.
     * Used to map Phase 1 results (keyed by CSV id) to DB products (id 1, 2, 3, ...).
     *
     * @return List of original CSV ids in seed row order, or empty list if seed file unavailable
     */
    private List<Integer> loadSeedProductIdsOrder() {
        List<Integer> ids = new ArrayList<>();
        try {
            ClassPathResource resource = new ClassPathResource("seed/products.csv");
            if (!resource.exists()) {
                return ids;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String header = reader.readLine();
                if (header == null) return ids;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    try {
                        String[] parts = parseCSVLine(line);
                        if (parts.length > 0) {
                            String first = parts[0].trim().replace("\"", "").replace("\uFEFF", "");
                            int id = Integer.parseInt(first);
                            ids.add(id);
                        }
                    } catch (Exception e) {
                        // skip malformed lines
                    }
                }
            }
            logger.info("  Loaded {} seed product IDs for Phase 1 mapping", ids.size());
        } catch (Exception e) {
            logger.warn("  Could not load seed products.csv for Phase 1 mapping: {}", e.getMessage());
        }
        return ids;
    }

    /**
     * Updates products in PostgreSQL database with ML categories
     * Maps Phase 1 results (keyed by original CSV id) to DB products (id 1,2,3...) using seed row order.
     * Only overwrites products that are uncategorized (null, blank, or "Unclassified"); leaves others unchanged.
     */
    private Map<String, Object> updateProductsInDatabase(Map<String, Object> readResults) {
        Map<String, Object> updateResults = new HashMap<>();
        int updated = 0;
        int skipped = 0;
        int notFound = 0;
        int errors = 0;

        @SuppressWarnings("unchecked")
        Map<Integer, Map<String, String>> productCategories =
            (Map<Integer, Map<String, String>>) readResults.get("product_categories");

        if (productCategories == null || productCategories.isEmpty()) {
            logger.warn("  No product categories to update");
            updateResults.put("success", false);
            updateResults.put("message", "No product categories found");
            return updateResults;
        }

        List<Integer> seedOrderedIds = loadSeedProductIdsOrder();
        final boolean useSeedMapping = !seedOrderedIds.isEmpty();

        if (useSeedMapping) {
            logger.info("  Updating products using seed order mapping (DB id 1 = CSV id {}, ...); only uncategorized products will be updated", seedOrderedIds.isEmpty() ? "?" : seedOrderedIds.get(0));
        } else {
            logger.info("  Updating products by CSV id / name fallback (seed/products.csv not found); only uncategorized products will be updated");
        }

        if (useSeedMapping) {
            for (int dbId = 1; dbId <= seedOrderedIds.size(); dbId++) {
                int csvId = seedOrderedIds.get(dbId - 1);
                Map<String, String> categories = productCategories.get(csvId);
                if (categories == null) continue;
                try {
                    Product product = productRepository.findById(dbId).orElse(null);
                    if (product != null) {
                        if (!isUncategorized(product)) {
                            skipped++;
                            continue;
                        }
                        String mainCategory = categories.get("main_category");
                        String subCategory = categories.get("sub_category");
                        if (mainCategory != null && !mainCategory.isEmpty()) {
                            product.setCategory(mainCategory);
                            product.setMlCategory(mainCategory);
                        }
                        if (subCategory != null && !subCategory.isEmpty()) {
                            product.setSubCategory(subCategory);
                        }
                        product.setUpdatedAt(LocalDateTime.now());
                        productRepository.save(product);
                        updated++;
                        if (updated % 100 == 0) {
                            logger.info("  Updated {} products so far...", updated);
                        }
                    } else {
                        notFound++;
                    }
                } catch (Exception e) {
                    errors++;
                    logger.error("  Error updating product DB id {} (CSV id {}): {}", dbId, csvId, e.getMessage());
                }
            }
        } else {
            for (Map.Entry<Integer, Map<String, String>> entry : productCategories.entrySet()) {
                try {
                    Integer productId = entry.getKey();
                    Map<String, String> categories = entry.getValue();

                    Product product = productRepository.findById(productId).orElse(null);
                    if (product == null) {
                        String productName = categories.get("product_name");
                        if (productName != null && !productName.isEmpty()) {
                            var byName = productRepository.findByProductNameContainingIgnoreCase(productName);
                            if (!byName.isEmpty()) {
                                product = byName.get(0);
                            }
                        }
                    }
                    if (product != null) {
                        if (!isUncategorized(product)) {
                            skipped++;
                            continue;
                        }
                        String mainCategory = categories.get("main_category");
                        String subCategory = categories.get("sub_category");

                        if (mainCategory != null && !mainCategory.isEmpty()) {
                            product.setCategory(mainCategory);
                            product.setMlCategory(mainCategory);
                        }
                        if (subCategory != null && !subCategory.isEmpty()) {
                            product.setSubCategory(subCategory);
                        }
                        product.setUpdatedAt(LocalDateTime.now());
                        productRepository.save(product);
                        updated++;
                        if (updated % 100 == 0) {
                            logger.info("  Updated {} products so far...", updated);
                        }
                    } else {
                        notFound++;
                        if (notFound <= 10) {
                            logger.debug("  Product ID {} not found in database (and no name match)", productId);
                        }
                    }
                } catch (Exception e) {
                    errors++;
                    logger.error("  Error updating product ID {}: {}", entry.getKey(), e.getMessage());
                }
            }
        }

        updateResults.put("success", true);
        updateResults.put("updated", updated);
        updateResults.put("skipped_already_categorized", skipped);
        updateResults.put("not_found", notFound);
        updateResults.put("errors", errors);
        updateResults.put("total_processed", productCategories.size());

        logger.info("  Product update complete: {} updated, {} skipped (already had category), {} not found, {} errors",
                    updated, skipped, notFound, errors);

        return updateResults;
    }

    /** True if product has no real category (null, blank, or "Unclassified"). Phase 1 only overwrites these. */
    private boolean isUncategorized(Product product) {
        String cat = product.getCategory();
        if (cat != null && !cat.isBlank() && !"Unclassified".equalsIgnoreCase(cat)) {
            return false;
        }
        String mlCat = product.getMlCategory();
        if (mlCat != null && !mlCat.isBlank() && !"Unclassified".equalsIgnoreCase(mlCat)) {
            return false;
        }
        return true;
    }

    /**
     * Creates a summary of the execution
     */
    private Map<String, Object> createSummary(Map<String, Object> userResults, 
                                               Map<String, Object> productResults) {
        Map<String, Object> summary = new HashMap<>();
        
        summary.put("users_updated", userResults.get("updated"));
        summary.put("users_not_found", userResults.get("not_found"));
        summary.put("users_errors", userResults.get("errors"));
        
        summary.put("products_updated", productResults.get("updated"));
        summary.put("products_not_found", productResults.get("not_found"));
        summary.put("products_errors", productResults.get("errors"));
        
        return summary;
    }

    /**
     * Runs Phase 2: Recommendation System
     * Executes Python scripts for hybrid recommendations (Collaborative + Content-Based + optional Neural Network),
     * trains models, and saves evaluation results to datasets/results/phase2/
     */
    public Map<String, Object> runPhase2() {
        logger.info("Phase 2: Recommendation System - Starting");
        Map<String, Object> results = new HashMap<>();

        try {
            Map<String, Object> executionResults = executePhase2Script();
            results.put("execution", executionResults);

            boolean success = Boolean.TRUE.equals(executionResults.get("success"));
            results.put("success", success);

            if (success) {
                results.put("message", "Phase 2 completed successfully. Models and evaluation saved to datasets/results/phase2/");
                logger.info("Phase 2 completed successfully");
            } else {
                results.put("message", "Phase 2 script execution failed");
                Object err = executionResults.get("error");
                if (err != null) results.put("error", err.toString());
            }
        } catch (Exception e) {
            logger.error("Error running Phase 2", e);
            results.put("success", false);
            results.put("error", e.getMessage());
        }

        return results;
    }

    /**
     * Executes Python script for Phase 2: run_all_phases.py --phase2
     */
    private Map<String, Object> executePhase2Script() {
        Map<String, Object> executionResults = new HashMap<>();

        try {
            Path projectRootPath = Paths.get(projectRoot);
            if (!Files.isDirectory(projectRootPath.resolve("src").resolve("phase2"))) {
                Path nested = projectRootPath.resolve("ML-eCommers-GitHub-9.2.26");
                if (Files.isDirectory(nested.resolve("src").resolve("phase2"))) {
                    projectRootPath = nested;
                    logger.info("  Using nested project root for Phase 2: {}", projectRootPath);
                } else {
                    Path fromList = resolveProjectRootWithPhase1(projectRootPath);
                    if (fromList != null && Files.isDirectory(fromList.resolve("src").resolve("phase2"))) {
                        projectRootPath = fromList;
                        logger.info("  Using nested project root for Phase 2: {}", projectRootPath);
                    }
                }
            }

            Path runAllPhasesScript = projectRootPath.resolve("run_all_phases.py");
            Path runAllInScripts = projectRootPath.resolve("scripts").resolve("run_all_phases.py");

            Path scriptToRun = null;
            String scriptName = null;

            if (Files.exists(runAllPhasesScript)) {
                scriptToRun = runAllPhasesScript;
                scriptName = "run_all_phases.py";
            } else if (Files.exists(runAllInScripts)) {
                scriptToRun = runAllInScripts;
                scriptName = "scripts/run_all_phases.py";
            }

            if (scriptToRun == null) {
                throw new IOException("run_all_phases.py not found at project root or scripts/");
            }

            logger.info("  Running Phase 2: {} with --phase2", scriptName);

            List<String> commandArgs = new ArrayList<>();
            commandArgs.add("python");
            commandArgs.add(scriptToRun.toString());
            commandArgs.add("--phase2");

            ProcessBuilder processBuilder = new ProcessBuilder(commandArgs);
            processBuilder.directory(projectRootPath.toFile());
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.info("  PHASE2: {}", line);
                }
            }

            StringBuilder errorOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new java.io.InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                    logger.warn("  PHASE2-ERR: {}", line);
                }
            }

            int exitCode = process.waitFor();
            executionResults.put("script", scriptName);
            executionResults.put("exit_code", exitCode);
            executionResults.put("success", exitCode == 0);
            executionResults.put("output", output.toString());
            executionResults.put("error_output", errorOutput.toString());
            if (exitCode != 0) {
                executionResults.put("error", "Script exited with code " + exitCode + ". Check output for details.");
            }

        } catch (Exception e) {
            logger.error("Error executing Phase 2 script", e);
            executionResults.put("success", false);
            executionResults.put("error", e.getMessage());
        }

        return executionResults;
    }

    /**
     * Runs Phase 3: Single Item Categorization
     * Executes Python script for single-item categorization validation (non-interactive).
     * For categorizing specific users/products, use POST /api/ml/user/{id}/categorize or /product/{id}/categorize.
     */
    public Map<String, Object> runPhase3() {
        logger.info("Phase 3: Single Item Categorization - Starting");
        Map<String, Object> results = new HashMap<>();

        try {
            Map<String, Object> executionResults = executePhase3Script();
            results.put("execution", executionResults);

            boolean success = Boolean.TRUE.equals(executionResults.get("success"));
            results.put("success", success);

            if (success) {
                results.put("message", "Phase 3 validation completed successfully.");
                logger.info("Phase 3 completed successfully");
            } else {
                results.put("message", "Phase 3 script execution failed");
                Object err = executionResults.get("error");
                if (err != null) results.put("error", err.toString());
            }
        } catch (Exception e) {
            logger.error("Error running Phase 3", e);
            results.put("success", false);
            results.put("error", e.getMessage());
        }

        return results;
    }

    /**
     * Executes Python script for Phase 3: run_all_phases.py --phase3
     */
    private Map<String, Object> executePhase3Script() {
        Map<String, Object> executionResults = new HashMap<>();

        try {
            Path projectRootPath = Paths.get(projectRoot);
            if (!Files.isDirectory(projectRootPath.resolve("src").resolve("phase3"))) {
                Path nested = projectRootPath.resolve("ML-eCommers-GitHub-9.2.26");
                if (Files.isDirectory(nested.resolve("src").resolve("phase3"))) {
                    projectRootPath = nested;
                    logger.info("  Using nested project root for Phase 3: {}", projectRootPath);
                } else {
                    Path fromList = resolveProjectRootWithPhase1(projectRootPath);
                    if (fromList != null && Files.isDirectory(fromList.resolve("src").resolve("phase3"))) {
                        projectRootPath = fromList;
                        logger.info("  Using nested project root for Phase 3: {}", projectRootPath);
                    }
                }
            }

            Path runAllPhasesScript = projectRootPath.resolve("run_all_phases.py");
            Path runAllInScripts = projectRootPath.resolve("scripts").resolve("run_all_phases.py");

            Path scriptToRun = null;
            String scriptName = null;

            if (Files.exists(runAllPhasesScript)) {
                scriptToRun = runAllPhasesScript;
                scriptName = "run_all_phases.py";
            } else if (Files.exists(runAllInScripts)) {
                scriptToRun = runAllInScripts;
                scriptName = "scripts/run_all_phases.py";
            }

            if (scriptToRun == null) {
                throw new IOException("run_all_phases.py not found at project root or scripts/");
            }

            logger.info("  Running Phase 3: {} with --phase3", scriptName);

            List<String> commandArgs = new ArrayList<>();
            commandArgs.add("python");
            commandArgs.add(scriptToRun.toString());
            commandArgs.add("--phase3");

            ProcessBuilder processBuilder = new ProcessBuilder(commandArgs);
            processBuilder.directory(projectRootPath.toFile());
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.info("  PHASE3: {}", line);
                }
            }

            StringBuilder errorOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new java.io.InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                    logger.warn("  PHASE3-ERR: {}", line);
                }
            }

            int exitCode = process.waitFor();
            executionResults.put("script", scriptName);
            executionResults.put("exit_code", exitCode);
            executionResults.put("success", exitCode == 0);
            executionResults.put("output", output.toString());
            executionResults.put("error_output", errorOutput.toString());
            if (exitCode != 0) {
                executionResults.put("error", "Script exited with code " + exitCode + ". Check output for details.");
            }

        } catch (Exception e) {
            logger.error("Error executing Phase 3 script", e);
            executionResults.put("success", false);
            executionResults.put("error", e.getMessage());
        }

        return executionResults;
    }

    /**
     * Categorizes a single user and updates the database
     * 
     * @param userId User ID to categorize
     * @return Categorization result with database update status
     */
    public Map<String, Object> categorizeSingleUser(int userId) {
        logger.info("Categorizing single user: {}", userId);
        Map<String, Object> results = new HashMap<>();
        
        try {
            // Step 1: Run Python script to categorize user
            Map<String, Object> pythonResult = executeSingleUserCategorization(userId);
            results.put("python_execution", pythonResult);
            
            if (!Boolean.TRUE.equals(pythonResult.get("success"))) {
                results.put("success", false);
                results.put("error", "Python script execution failed");
                return results;
            }
            
            // Step 2: Parse result and update database
            @SuppressWarnings("unchecked")
            Map<String, Object> categorizationResult = (Map<String, Object>) pythonResult.get("result");
            if (categorizationResult != null) {
                String category = (String) categorizationResult.get("category");
                
                // Update user in database
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    user.setMlCategory(category);
                    user.setSegment(UserSegment.CLASSIFIED);
                    user.setLastClassifiedAt(LocalDateTime.now());
                    userRepository.save(user);
                    
                    results.put("success", true);
                    results.put("user_id", userId);
                    results.put("category", category);
                    results.put("database_updated", true);
                    results.put("details", categorizationResult);
                    
                    logger.info("User {} categorized as '{}' and updated in database", userId, category);
                } else {
                    results.put("success", false);
                    results.put("error", "User not found in database");
                    results.put("category", category);
                }
            } else {
                results.put("success", false);
                results.put("error", "No categorization result from Python script");
            }
            
        } catch (Exception e) {
            logger.error("Error categorizing user {}", userId, e);
            results.put("success", false);
            results.put("error", e.getMessage());
        }
        
        return results;
    }

    /**
     * Categorizes a single product and updates the database
     * 
     * @param productId Product ID to categorize
     * @return Categorization result with database update status
     */
    public Map<String, Object> categorizeSingleProduct(int productId) {
        logger.info("Categorizing single product: {}", productId);
        Map<String, Object> results = new HashMap<>();
        
        try {
            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) {
                results.put("success", false);
                results.put("error", "Product " + productId + " not found in database");
                return results;
            }

            // Step 1: Try ML service (Flask) first – fast (model kept in memory). If down, fallback to spawning Python.
            Map<String, Object> categorizationResult = null;
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("product_name", product.getProductName() != null ? product.getProductName() : "");
                requestBody.put("description", product.getDescription() != null ? product.getDescription() : "");
                requestBody.put("price", (double) product.getPrice());
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                @SuppressWarnings("unchecked")
                Map<String, Object> resp = restTemplate.postForObject(mlServiceUrl + "/categorize-product", entity, Map.class);
                if (resp != null && (resp.get("main_category") != null || resp.get("sub_category") != null)) {
                    categorizationResult = resp;
                    results.put("source", "ml_service_http");
                    logger.debug("Product {} categorized via ML service (HTTP)", productId);
                }
            } catch (Exception e) {
                logger.debug("ML service unavailable, falling back to Python spawn: {}", e.getMessage());
            }

            if (categorizationResult == null) {
                if (!mlCategorizeFallbackToPython) {
                    results.put("success", false);
                    results.put("error", "ML service unavailable. Start ml_service for fast per-product categorization, or run Phase 1 (POST /api/ml/phase1) to categorize in batch.");
                    results.put("product_id", productId);
                    logger.info("Product {} not categorized (ml_service down, fallback disabled). Run Phase 1 or start ml_service.", productId);
                    return results;
                }
                Map<String, Object> pythonResult = executeSingleProductCategorization(productId);
                results.put("python_execution", pythonResult);
                if (!Boolean.TRUE.equals(pythonResult.get("success"))) {
                    results.put("success", false);
                    Object pyErr = pythonResult.get("error");
                    Object pyOut = pythonResult.get("output");
                    results.put("error", pyErr != null ? pyErr.toString() : "Python script execution failed");
                    if (pyOut != null) results.put("output", pyOut);
                    return results;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> fromPython = (Map<String, Object>) pythonResult.get("result");
                categorizationResult = fromPython;
            }
            
            // Step 2: Parse result and update database
            if (categorizationResult != null) {
                String mainCategory = (String) categorizationResult.get("main_category");
                String subCategory = (String) categorizationResult.get("sub_category");
                
                // Update product in database (product already fetched above)
                boolean updated = false;
                if (mainCategory != null && !mainCategory.isEmpty()) {
                    product.setCategory(mainCategory);
                    product.setMlCategory(mainCategory);
                    updated = true;
                }
                if (subCategory != null && !subCategory.isEmpty()) {
                    product.setSubCategory(subCategory);
                    updated = true;
                }
                @SuppressWarnings("unchecked")
                List<String> mlTags = (List<String>) categorizationResult.get("tags");
                if (mlTags != null && !mlTags.isEmpty()) {
                    product.setTags(mlTags);
                    updated = true;
                }
                if (updated) {
                    product.setUpdatedAt(LocalDateTime.now());
                    productRepository.save(product);
                }
                results.put("success", true);
                results.put("product_id", productId);
                results.put("main_category", mainCategory);
                results.put("sub_category", subCategory);
                results.put("database_updated", true);
                results.put("details", categorizationResult);
                logger.info("Product {} categorized as '{}' / '{}' and updated in database",
                        productId, mainCategory, subCategory);
            } else {
                results.put("success", false);
                results.put("error", "No categorization result from Python script");
            }
            
        } catch (Exception e) {
            logger.error("Error categorizing product {}", productId, e);
            results.put("success", false);
            results.put("error", e.getMessage());
        }
        
        return results;
    }

    /**
     * Executes Python script to categorize a single user
     */
    private Map<String, Object> executeSingleUserCategorization(int userId) {
        Map<String, Object> executionResults = new HashMap<>();
        
        try {
            Path projectRootPath = Paths.get(projectRoot);
            Path phase3Script = projectRootPath.resolve("src").resolve("phase3").resolve("single_item_categorization.py");
            
            if (!Files.exists(phase3Script)) {
                throw new IOException("Phase 3 script not found: " + phase3Script);
            }
            
            // Create output file for JSON result
            Path outputFile = Files.createTempFile("user_categorization_", ".json");
            
            // Create a temporary Python script to call the categorization
            Path tempScript = Files.createTempFile("categorize_user_", ".py");
            String scriptContent = String.format(
                "import sys\n" +
                "from pathlib import Path\n" +
                "import json\n" +
                "sys.path.insert(0, str(Path('%s') / 'src' / 'phase3'))\n" +
                "sys.path.insert(0, str(Path('%s') / 'src' / 'phase1'))\n" +
                "from single_item_categorization import SingleItemCategorization\n" +
                "\n" +
                "try:\n" +
                "    categorizer = SingleItemCategorization('%s')\n" +
                "    result = categorizer.categorize(item_id=%d, item_type='user', use_model=True)\n" +
                "    with open(r'%s', 'w', encoding='utf-8') as f:\n" +
                "        json.dump(result, f, indent=2, default=str)\n" +
                "    print('SUCCESS')\n" +
                "except Exception as e:\n" +
                "    with open(r'%s', 'w', encoding='utf-8') as f:\n" +
                "        json.dump({'error': str(e)}, f)\n" +
                "    print('ERROR:', str(e))\n" +
                "    sys.exit(1)\n",
                projectRootPath.toString().replace("\\", "/"),
                projectRootPath.toString().replace("\\", "/"),
                projectRootPath.toString().replace("\\", "/"),
                userId,
                outputFile.toString().replace("\\", "/"),
                outputFile.toString().replace("\\", "/")
            );
            
            Files.write(tempScript, scriptContent.getBytes());
            
            ProcessBuilder processBuilder = new ProcessBuilder("python", tempScript.toString());
            processBuilder.directory(projectRootPath.toFile());
            Process process = processBuilder.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = process.waitFor();
            
            // Read JSON from file
            Map<String, Object> result = null;
            if (Files.exists(outputFile)) {
                try {
                    String jsonContent = Files.readString(outputFile);
                    result = parseSimpleJson(jsonContent);
                } catch (Exception e) {
                    logger.warn("Could not parse JSON from file: {}", e.getMessage());
                }
            }
            
            executionResults.put("success", exitCode == 0);
            executionResults.put("exit_code", exitCode);
            executionResults.put("output", output.toString());
            executionResults.put("result", result);
            
            // Clean up temp files
            Files.deleteIfExists(tempScript);
            Files.deleteIfExists(outputFile);
            
        } catch (Exception e) {
            logger.error("Error executing single user categorization", e);
            executionResults.put("success", false);
            executionResults.put("error", e.getMessage());
        }
        
        return executionResults;
    }

    /**
     * Executes Python script to categorize a single product.
     * Uses categorize_new_product() so it works for products NOT in Phase 1 CSV (e.g. newly added via Admin).
     * Passes product name, description, price from DB to the Python script.
     */
    private Map<String, Object> executeSingleProductCategorization(int productId) {
        Map<String, Object> executionResults = new HashMap<>();
        
        try {
            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) {
                executionResults.put("success", false);
                executionResults.put("error", "Product " + productId + " not found in database");
                return executionResults;
            }
            
            Path projectRootPath = Paths.get(projectRoot);
            Path phase3Script = projectRootPath.resolve("src").resolve("phase3").resolve("single_item_categorization.py");
            
            if (!Files.exists(phase3Script)) {
                throw new IOException("Phase 3 script not found: " + phase3Script);
            }
            
            // Create product JSON file (safe for Python to read - avoids escaping issues)
            Path productJsonFile = Files.createTempFile("product_for_ml_", ".json");
            Map<String, Object> productData = new HashMap<>();
            productData.put("id", productId);
            productData.put("product_name", product.getProductName() != null ? product.getProductName() : "");
            productData.put("description", product.getDescription() != null ? product.getDescription() : "");
            productData.put("price", (double) product.getPrice());
            try (BufferedWriter w = Files.newBufferedWriter(productJsonFile, StandardCharsets.UTF_8)) {
                w.write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(productData));
            }
            
            // Create output file for JSON result
            Path outputFile = Files.createTempFile("product_categorization_", ".json");
            
            // Escape paths for Python string (backslashes -> forward slashes)
            String projectRootEscaped = projectRootPath.toString().replace("\\", "/");
            String productJsonPathEscaped = productJsonFile.toString().replace("\\", "/");
            String outputPathEscaped = outputFile.toString().replace("\\", "/");
            
            // Python: read product from JSON, use categorize_new_product for products not in Phase 1 CSV
            String scriptContent = 
                "import sys\n" +
                "from pathlib import Path\n" +
                "import json\n" +
                "sys.path.insert(0, str(Path('" + projectRootEscaped + "') / 'src' / 'phase3'))\n" +
                "sys.path.insert(0, str(Path('" + projectRootEscaped + "') / 'src'))\n" +
                "from single_item_categorization import SingleItemCategorization\n" +
                "\n" +
                "try:\n" +
                "    with open(r'" + productJsonPathEscaped + "', encoding='utf-8') as f:\n" +
                "        pd = json.load(f)\n" +
                "    name = pd.get('product_name', pd.get('name', '')) or ''\n" +
                "    desc = pd.get('description', '') or ''\n" +
                "    price = float(pd.get('price', 0) or 0)\n" +
                "    categorizer = SingleItemCategorization('" + projectRootEscaped + "')\n" +
                "    try:\n" +
                "        result = categorizer.categorize(item_id=" + productId + ", item_type='product', use_model=True)\n" +
                "        out = {'main_category': result.get('main_category',''), 'sub_category': result.get('sub_category',''), 'method': result.get('method','model')}\n" +
                "    except (ValueError, KeyError) as e:\n" +
                "        if 'not found' in str(e).lower() or 'not in dataset' in str(e).lower():\n" +
                "            raw = categorizer.categorize_new_product(name, desc, price, use_model=True)\n" +
                "            out = {'main_category': raw.get('predicted_main_category',''), 'sub_category': raw.get('predicted_sub_category',''), 'method': raw.get('method','model')}\n" +
                "        else:\n" +
                "            raise\n" +
                "    with open(r'" + outputPathEscaped + "', 'w', encoding='utf-8') as f:\n" +
                "        json.dump(out, f, indent=2)\n" +
                "    print('SUCCESS')\n" +
                "except Exception as e:\n" +
                "    with open(r'" + outputPathEscaped + "', 'w', encoding='utf-8') as f:\n" +
                "        json.dump({'error': str(e)}, f)\n" +
                "    print('ERROR:', str(e))\n" +
                "    sys.exit(1)\n";
            
            Path tempScript = Files.createTempFile("categorize_product_", ".py");
            Files.write(tempScript, scriptContent.getBytes(StandardCharsets.UTF_8));
            
            Process process;
            try {
                ProcessBuilder pb = new ProcessBuilder("python", tempScript.toString());
                pb.directory(projectRootPath.toFile());
                process = pb.start();
            } catch (IOException e) {
                if (System.getProperty("os.name", "").toLowerCase().startsWith("win")) {
                    ProcessBuilder pb = new ProcessBuilder("py", "-3", tempScript.toString());
                    pb.directory(projectRootPath.toFile());
                    process = pb.start();
                } else {
                    throw e;
                }
            }
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = process.waitFor();
            
            // Read JSON from file
            Map<String, Object> result = null;
            if (Files.exists(outputFile)) {
                try {
                    String jsonContent = Files.readString(outputFile);
                    result = parseSimpleJson(jsonContent);
                } catch (Exception e) {
                    logger.warn("Could not parse JSON from file: {}", e.getMessage());
                }
            }
            
            executionResults.put("success", exitCode == 0);
            executionResults.put("exit_code", exitCode);
            executionResults.put("output", output.toString());
            executionResults.put("result", result);
            
            // Clean up temp files
            Files.deleteIfExists(tempScript);
            Files.deleteIfExists(productJsonFile);
            Files.deleteIfExists(outputFile);
            
        } catch (Exception e) {
            logger.error("Error executing single product categorization", e);
            executionResults.put("success", false);
            executionResults.put("error", e.getMessage());
        }
        
        return executionResults;
    }

    /**
     * Simple JSON parser for Python output (basic implementation)
     * Extracts key fields from JSON string
     */
    private Map<String, Object> parseSimpleJson(String jsonStr) {
        Map<String, Object> result = new HashMap<>();
        try {
            // Extract key fields using regex patterns
            extractJsonField(jsonStr, "category", result);
            extractJsonField(jsonStr, "main_category", result);
            extractJsonField(jsonStr, "sub_category", result);
            extractJsonField(jsonStr, "item_id", result);
            extractJsonField(jsonStr, "item_type", result);
            extractJsonField(jsonStr, "method", result);
            
            // Extract from details object if present
            if (jsonStr.contains("\"details\"")) {
                extractJsonField(jsonStr, "total_clicks", result);
                extractJsonField(jsonStr, "total_purchases", result);
                extractJsonField(jsonStr, "unique_products", result);
                extractJsonField(jsonStr, "engagement_score", result);
                extractJsonField(jsonStr, "product_name", result);
                extractJsonField(jsonStr, "price", result);
            }
            
            // If error field exists, extract it
            if (jsonStr.contains("\"error\"")) {
                extractJsonField(jsonStr, "error", result);
            }
            
        } catch (Exception e) {
            logger.warn("Could not parse JSON: {}", e.getMessage());
            result.put("raw", jsonStr);
        }
        return result;
    }
    
    /**
     * Helper method to extract a field from JSON string
     */
    private void extractJsonField(String jsonStr, String fieldName, Map<String, Object> result) {
        try {
            String pattern = "\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(jsonStr);
            if (m.find()) {
                result.put(fieldName, m.group(1));
            } else {
                // Try numeric value
                pattern = "\"" + fieldName + "\"\\s*:\\s*([0-9.]+)";
                p = java.util.regex.Pattern.compile(pattern);
                m = p.matcher(jsonStr);
                if (m.find()) {
                    try {
                        if (m.group(1).contains(".")) {
                            result.put(fieldName, Double.parseDouble(m.group(1)));
                        } else {
                            result.put(fieldName, Integer.parseInt(m.group(1)));
                        }
                    } catch (NumberFormatException e) {
                        result.put(fieldName, m.group(1));
                    }
                }
            }
        } catch (Exception e) {
            // Ignore extraction errors for individual fields
        }
    }
}
