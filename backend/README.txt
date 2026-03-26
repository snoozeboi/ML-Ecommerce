## Backend Requirements

- [Java 17+](https://adoptium.net/)
- [Maven](https://maven.apache.org/download.cgi)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [Git](https://git-scm.com/downloads)
- [Lombok](Install Lombok plugin in your IDE : File -> Settings -> Plugins -> Search for Lombok)

## Backend Setup

1. Create an account on https://github.com/
2. In your IDE, Open git console ( example : https://i.gyazo.com/cec0f087b60c7055a30bb3f0958822d0.png )
3. Clone the repository from GitHub, then open the project in your IDE (e.g. the folder containing backend and frontend).
	git clone https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPOSITORY_NAME.git
	cd <your-project-folder>
4. Run Docker
5. In IDE Terminal, go to the **backend** folder (where docker-compose.yml is) and run: docker compose pull (it will pull postgresql, elastic, rabbit, pgadmin).
6. Before you run docker compose, check if your ports are free with PowerShell: netstat -ano | findstr :5050 for each port 9200, 5672, 5050, 5433. Skip to step 7 if ports are free.

*If the ports are taken,* add a run config in your IDE (IntelliJ: Run > Edit Configurations > + > Application). Name: Backend (or another name), Main class: com.shop.ecommerce.EcommerceApplication, Module: backend. In "Environment variables" set e.g. POSTGRES_PORT=5434;RABBIT_PORT=5673;ES_URI=http://localhost:9201 to your free ports. Click OK.

7. From the **backend** folder run: docker compose up -d (to stop: docker compose down).
8. When the containers are up, run the backend: in IntelliJ choose the **Backend** run config (main class EcommerceApplication, no VM options) and run. Use the **seed** config only when you want to seed the DB (VM option: -Dspring.profiles.active=seed,init-ratings).
9. **ML service (Flask)** starts automatically in the background when the backend starts (if not already running). Product categorization is then fast. Logs: ml_service_out.log and ml_service_err.log in the project root. To disable auto-start (e.g. you run ml_service via Docker): set ml.service.auto-start=false or ML_SERVICE_AUTO_START=false.

ML categorization behavior:
- ml.categorize.on.save=true (default): each new/updated Unclassified product is sent for ML categorization. Set to false to skip per-product ML and run Phase 1 (POST /api/ml/phase1) periodically instead.
- ml.categorize.fallback-to-python=false (default): when ml_service is down, do not spawn Python (avoids ~30s per product). Products stay Unclassified until Phase 1 or ml_service is used. Set to true to keep the old slow per-product Python fallback.
- Single-product ML uses the demo catalog (products_10000.csv) as reference so existing product categories are preserved; only the new or updated product is categorized. Phase 1 (batch) still uses the live DB export when you run POST /api/ml/phase1.

One-time fix: If your DB has wrong categories but datasets/results/phase1/products_with_categories.csv has the correct ones, call POST /api/ml/sync-products-from-csv once. This applies the CSV to the DB without running Phase 1. If CSV row order matches DB product id order (line 2 → product id 1, line 3 → id 2, …), use ?byRowOrder=true: POST /api/ml/sync-products-from-csv?byRowOrder=true. After that, restart ml_service and use single-product ML only (no need to run Phase 1 again). To match the DB to the seed file (same categories as seed/products.csv by row order), call POST /api/ml/sync-categories-from-seed once.

Recommended workflow: Use the one-time sync only for products already in your DB (e.g. POST /api/ml/sync-categories-from-seed). Do not run Phase 1 or sync again for normal use. New products are then categorized automatically by single-item ML, which uses existing DB categories as reference—so the one-time sync fixes current products; new products get ML-based categories from that baseline.

Optional: Run ml_service manually (e.g. for debugging): cd backend && python ml_service/app.py. It will listen on http://localhost:5000.

## "Module not specified" when running EcommerceApplication

If the main class shows "Module not specified" in the run configuration:

1. Go to Run → Edit Configurations (or the dropdown next to Run → Edit Configurations).
2. Select your Application configuration (e.g. the one with Main class: com.shop.ecommerce.EcommerceApplication). If none exists, click + → Application and set Main class to com.shop.ecommerce.EcommerceApplication.
3. In the same dialog, find the **Module** (or "Use classpath of module") field.
4. Open the dropdown and select the **backend** module (the Maven module whose pom.xml has <artifactId>backend</artifactId>). It may appear as "backend" or with a prefix like "ML-eCommers-GitHub-15.2.26.backend" depending on how the project was opened.
5. Click Apply, then OK.

If "backend" does not appear in the list, open the project from the folder that contains the backend pom.xml (e.g. open backend/ as the root, or use File → New → Project from Existing Sources and point to the backend folder) so IntelliJ registers the backend module, then repeat the steps above.

## Links

RabbitMQ UI → http://localhost:15672
User: admin
Password: admin123

Elasticsearch API → http://localhost:9200

Swagger UI → http://localhost:8080/swagger-ui.html (after backend starts)
Swagger is used to check schemas and test the backend without the frontend (send mappings/jsons and see responses)

## Optional: Set random product ratings (0.0 - 5.0)

To fill the product `rating` field with random values for all existing products, run the backend once with profile `init-ratings`:
  mvn spring-boot:run -Dspring-boot.run.profiles=init-ratings
(Or in IDE: add VM/active profile: init-ratings, then run.) This updates all products and exits.

## Why is users.ml_category empty?

The column is filled only when you run the Phase 1 ML pipeline (POST /api/ml/phase1) or single-user categorization (POST /api/ml/user/{userId}/categorize). See ../docs/ML_API_DOCUMENTATION.md for details.

pgAdmin (database UI) → http://localhost:5050
User: admin@admin.com
Password: admin123

## How to set up pgAdmin connection to the database container

1. Connect to http://localhost:5050 and enter the credentials above
2. Under object explorer, right click the server -> register -> server...
3. Enter whichever name you want, in the connections tab -> Host name/address: postgres (this is the container name from docker-compose.yml)
                                                            Port: 5432
                                                            Username: admin
                                                            Password: admin123 (check “Save Password”)

4. Once you connect the UI to the database, you can see the tables at : Expand Databases → ecommerce_db → Schemas → public → Tables
                                                                        You will see a list of all your tables (users, products, cart_items, etc.).
                                                                        Right click a table → View/Edit Data → All Rows