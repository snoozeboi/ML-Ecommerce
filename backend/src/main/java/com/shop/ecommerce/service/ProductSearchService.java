package com.shop.ecommerce.service;

import co.elastic.clients.elasticsearch._types.query_dsl.FieldValueFactorModifier;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import com.shop.ecommerce.dto.ProductSuggestionDto;
import com.shop.ecommerce.dto.SuggestResponseDto;
import com.shop.ecommerce.search.ProductDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductSearchService {

  private final ElasticsearchOperations elasticsearchOperations;

  public SuggestResponseDto suggest(String q, String category) {
    String queryText = (q == null) ? "" : q.trim();
    if (queryText.isBlank()) {
      return new SuggestResponseDto("", List.of());
    }

    NativeQuery springQuery = NativeQuery.builder()
            .withQuery(qb -> qb.functionScore(fs -> fs
                    .query(base -> base.bool(b -> {

                      // SHOULD #1, exact search
                      b.should(s -> s.multiMatch(mm -> mm
                              .query(queryText)
                              .fields("productName^5", "tags^3", "description")
                              .operator(Operator.Or)
                      ));

                      // SHOULD #2, search with fuzziness
                      if (queryText.length() >= 4) {
                        b.should(s -> s.multiMatch(mm -> mm
                                .query(queryText)
                                .fields("productName^5", "tags^3", "description")
                                .fuzziness("AUTO")
                                .prefixLength(1)
                        ));
                      }

                      // returns only if at least 1 is applied.
                      b.minimumShouldMatch("1");

                      if (category != null && !category.isBlank()) {
                        b.filter(f -> f.term(t -> t.field("category.keyword").value(category)));
                      }

                      return b;
                    }))

                    // Multiply the scores
                    .scoreMode(FunctionScoreMode.Multiply)
                    .boostMode(FunctionBoostMode.Multiply)

                    // rating dominates (normalized)
                    .functions(f -> f.fieldValueFactor(v -> v
                            .field("rating")
                            .factor(0.6)          // rating ~ 0–5 → multiplier ~0–3
                            .missing(1.0)
                    ))

                    // views are supportive, not dominant
                    .functions(f -> f.fieldValueFactor(v -> v
                            .field("views")
                            .modifier(FieldValueFactorModifier.Log1p)
                            .factor(0.15)
                            .missing(1.0)
                    ))
            ))
            .withPageable(PageRequest.of(0, 10))
            .build();

    SearchHits<ProductDocument> hits =
            elasticsearchOperations.search(springQuery, ProductDocument.class);

    // DEBUG
//    System.out.println("TOTAL HITS=" + hits.getTotalHits());
//    hits.getSearchHits().stream().limit(10).forEach(h ->
//            System.out.println(
//                    "ES score=" + h.getScore()
//                            + " id=" + (h.getContent() == null ? "null" : h.getContent().getId())
//                            + " rating=" + (h.getContent() == null ? "null" : h.getContent().getRating())
//                            + " views=" + (h.getContent() == null ? "null" : h.getContent().getViews())
//                            + " name=" + (h.getContent() == null ? "null" : h.getContent().getProductName())
//            )
//    );

    List<ProductSuggestionDto> suggestions = hits.getSearchHits().stream()
            .map(h -> toSuggestion(h.getContent()))
            .toList();

    return new SuggestResponseDto(queryText, suggestions);
  }

  private ProductSuggestionDto toSuggestion(ProductDocument p) {
    float price = (p.getPrice() == null) ? 0f : p.getPrice();
    Integer discountPercent = null;

    float finalPrice = (discountPercent == null)
            ? price
            : price * (100 - discountPercent) / 100.0f;

    return new ProductSuggestionDto(
            p.getId() == null ? 0L : p.getId(),
            p.getProductName(),   // ES field
            p.getCategory(),
            p.getRating(),
            price,
            finalPrice,
            discountPercent,
            p.getImageUrl()
    );
  }
}