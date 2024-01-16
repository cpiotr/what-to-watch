package pl.ciruk.whattowatch.boot.boundary;

import pl.ciruk.whattowatch.boot.cache.RedisCache;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

@Named
@Path("/cache")
public class Cache {
    private final RedisCache redisCache;

    @Inject
    public Cache(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    @POST
    @Produces("application/json")
    CacheInvalidationResult invalidate(@QueryParam("keyExpression") String keyExpression) {
        long count = redisCache.removeAll(keyExpression);
        return CacheInvalidationResult.builder()
                .keyExpression(keyExpression)
                .invalidatedCount(count)
                .build();
    }
}
