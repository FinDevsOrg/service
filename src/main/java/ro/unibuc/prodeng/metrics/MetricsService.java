package ro.unibuc.prodeng.metrics;

import com.mongodb.client.MongoClient;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import org.bson.Document;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ro.unibuc.prodeng.repository.WalletRepository;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MetricsService {
    private final MeterRegistry registry;
    private final Counter userCreatedCounter;
    private final Counter usersRetrievedCounter;
    private final Timer requestTimer;
    private final AtomicInteger dbConnectionsActive = new AtomicInteger(0);
    private final WalletRepository walletRepository;
    private final ObjectProvider<MongoClient> mongoClientProvider;

    public MetricsService(MeterRegistry registry, WalletRepository walletRepository, ObjectProvider<MongoClient> mongoClientProvider) {
        this.registry = registry;
        this.walletRepository = walletRepository;
        this.mongoClientProvider = mongoClientProvider;

        //counter
        this.userCreatedCounter = Counter.builder("app_users_created")
                .description("Total number of users created")
                .register(registry);

        this.usersRetrievedCounter = Counter.builder("app_users_retrieved")
            .description("Total number of get users requests")
            .register(registry);

        
        //histrogram
        this.requestTimer = Timer.builder("app_request_duration_seconds")
                .description("API endpoint response time")
                .publishPercentiles(0.5, 0.95)
                .register(registry);

        //gauge
        Gauge.builder("app_db_connections_active", dbConnectionsActive, AtomicInteger::get)
                .description("Currently active database connections")
                .register(registry);


        //gauge
        Gauge.builder("app_items_in_cart", this, svc -> svc.computeItemsInCart())
                .description("Current number of items across all carts (domain-specific)")
                .register(registry);
    }

    public void recordUserCreated() {
        userCreatedCounter.increment();
    }

    public void recordUsersRetrieved() {
        usersRetrievedCounter.increment();
    }

    public void recordRequestDuration(long durationNanos) {
        try {
            requestTimer.record(durationNanos, TimeUnit.NANOSECONDS);
        } catch (Exception ignored) {
        }
    }

    public void recordError(String errorType) {
        try {
            registry.counter("app_errors", "type", errorType == null ? "unknown" : errorType).increment();
        } catch (Exception ignored) {
        }
    }

    private double computeItemsInCart() {
        try {
            // domain-specific: sum transactions across wallets (used as example of items)
            return walletRepository.findAll().stream()
                    .flatMap(w -> w.accounts().stream())
                    .mapToInt(a -> a.transactions() == null ? 0 : a.transactions().size())
                    .sum();
        } catch (Exception e) {
            return 0;
        }
    }

    @Scheduled(fixedDelayString = "10000")
    public void updateDbConnections() {
        try {
            MongoClient mongoClient = mongoClientProvider.getIfAvailable();
            if (mongoClient == null) {
                dbConnectionsActive.set(0);
                return;
            }

            Document status = mongoClient.getDatabase("admin").runCommand(new Document("serverStatus", 1));
            Object connObj = status.get("connections");
            if (connObj instanceof Document) {
                Document conn = (Document) connObj;
                Object current = conn.get("current");
                if (current instanceof Number) {
                    dbConnectionsActive.set(((Number) current).intValue());
                    return;
                }
            }
        } catch (Exception ignored) {
        }
        dbConnectionsActive.set(0);
    }
}
