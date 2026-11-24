package uz.demo.dockerlab.batchlab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;


import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.batch.core.configuration.annotation.StepScope;

import org.springframework.core.io.Resource;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Locale;

@Configuration
@ConditionalOnProperty(
        prefix = "lab.batch549",
        name = "enabled",
        havingValue = "true"
)
public class ProductImport549Config {

    private static final Logger log =
            LoggerFactory.getLogger(ProductImport549Config.class);

    // =====================================
    // 1. READER
    // =====================================

    @Bean
    @StepScope
    public FlatFileItemReader<ProductCsvRow> productCsvReader549(
            @Value("${lab.batch549.input-file}") Resource resource
    ) {
        return new FlatFileItemReaderBuilder<ProductCsvRow>()
                .name("productCsvReader549")
                .resource(resource)
                .linesToSkip(1)
                .delimited()
                .names("id", "name", "price")
                .fieldSetMapper(fields -> new ProductCsvRow(
                        fields.readLong("id"),
                        fields.readString("name"),
                        new BigDecimal(fields.readString("price"))
                ))
                .build();
    }

    // =====================================
    // 2. PROCESSOR
    // =====================================

    @Bean
    public ItemProcessor<ProductCsvRow, ImportProduct>
    productProcessor549(
            @Value("${lab.batch549.fail-at-id:0}") long failAtId
    ) {
        return row -> {

            if (row.id() == failAtId) {
                throw new IllegalStateException(
                        "SIMULATED FAILURE at product " + row.id()
                );
            }

            if (row.id() <= 0) {
                throw new IllegalArgumentException(
                        "Invalid product ID: " + row.id()
                );
            }

            if (row.name() == null || row.name().isBlank()) {
                throw new IllegalArgumentException(
                        "Empty product name: " + row.id()
                );
            }

            if (row.price() == null ||
                    row.price().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Invalid price for product: " + row.id()
                );
            }

            String normalizedName = row.name()
                    .trim()
                    .toUpperCase(Locale.ROOT);

            return new ImportProduct(
                    row.id(),
                    normalizedName,
                    row.price()
            );
        };
    }

    // =====================================
    // 3. WRITER
    // =====================================

    @Bean
    public JdbcBatchItemWriter<ImportProduct> productWriter549(
            DataSource dataSource
    ) {
        String sql = """
                INSERT INTO batch_import_products
                    (id, name, price)
                VALUES (?, ?, ?)
                ON CONFLICT (id)
                DO UPDATE SET
                    name = EXCLUDED.name,
                    price = EXCLUDED.price
                """;

        return new JdbcBatchItemWriterBuilder<ImportProduct>()
                .dataSource(dataSource)
                .sql(sql)
                .itemPreparedStatementSetter((item, ps) -> {
                    ps.setLong(1, item.id());
                    ps.setString(2, item.name());
                    ps.setBigDecimal(3, item.price());
                })
                .build();
    }

    // =====================================
    // 4. STEP
    // =====================================

    @Bean
    public Step productImportStep549(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<ProductCsvRow> productCsvReader549,
            ItemProcessor<ProductCsvRow, ImportProduct> productProcessor549,
            JdbcBatchItemWriter<ImportProduct> productWriter549,
            @Value("${lab.batch549.chunk-size:1000}") int chunkSize
    ) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException(
                    "Chunk size must be positive"
            );
        }

        return new StepBuilder(
                "productImportStep549",
                jobRepository
        )
                .<ProductCsvRow, ImportProduct>chunk(chunkSize)
                .transactionManager(transactionManager)
                .reader(productCsvReader549)
                .processor(productProcessor549)
                .writer(productWriter549)

                .faultTolerant()

                .skip(IllegalArgumentException.class)
                .skipLimit(10)

                .retry(TransientDataAccessException.class)
                .retryLimit(3)

                .build();
    }

    // =====================================
    // 5. JOB
    // =====================================

    @Bean
    public Job productImportJob549(
            JobRepository jobRepository,
            Step productImportStep549
    ) {
        return new JobBuilder(
                "productImportJob549",
                jobRepository
        )
                .start(productImportStep549)
                .build();
    }
}