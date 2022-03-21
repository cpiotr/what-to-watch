package pl.ciruk.whattowatch.boot.boundary;

import pl.ciruk.whattowatch.boot.cache.RedisCache;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

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
