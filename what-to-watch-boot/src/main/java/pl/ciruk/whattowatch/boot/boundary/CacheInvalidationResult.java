package pl.ciruk.whattowatch.boot.boundary;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CacheInvalidationResult(
        @JsonProperty String keyExpression,
        @JsonProperty Long invalidedCount) {
    static CacheInvalidationResultBuilder builder() {
        return new CacheInvalidationResultBuilder();
    }

    static class CacheInvalidationResultBuilder {
        private String keyExpression;
        private Long invalidatedCount;

        CacheInvalidationResultBuilder keyExpression(String keyExpression) {
            this.keyExpression = keyExpression;
            return this;
        }

        CacheInvalidationResultBuilder invalidatedCount(Long invalidatedCount) {
            this.invalidatedCount = invalidatedCount;
            return this;
        }

        pl.ciruk.whattowatch.boot.boundary.CacheInvalidationResult build() {
            return new pl.ciruk.whattowatch.boot.boundary.CacheInvalidationResult(keyExpression, invalidatedCount);
        }
    }
}
