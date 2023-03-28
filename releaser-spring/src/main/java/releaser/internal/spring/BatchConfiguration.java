/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package releaser.internal.spring;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import releaser.internal.ReleaserProperties;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.Jackson2ExecutionContextStringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
class BatchConfiguration {

	@Bean
	TaskExecutor batchTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(4);
		executor.initialize();
		return executor;
	}

	@Bean
	@ConditionalOnMissingBean(BuildReportHandler.class)
	SpringBatchBuildReportHandler springBatchBuildReportHandler(JobExplorer jobExplorer) {
		return new SpringBatchBuildReportHandler(jobExplorer);
	}

	@Bean
	@ConditionalOnMissingBean(ExecutionResultHandler.class)
	SpringBatchExecutionResultHandler springBatchExecutionResultHandler(BuildReportHandler buildReportHandler,
			ConfigurableApplicationContext context) {
		return new SpringBatchExecutionResultHandler(buildReportHandler, context);
	}

	@Bean
	@ConditionalOnMissingBean
	FlowRunnerTaskExecutorSupplier defaultFlowRunnerTaskExecutorSupplier() {
		return new FlowRunnerTaskExecutorSupplier() {
		};
	}

	@Bean
	@ConditionalOnMissingBean
	FlowRunner flowRunner(JobRepository jobRepository, PlatformTransactionManager manager,
			ProjectsToRunFactory projectsToRunFactory, JobLauncher jobLauncher,
			FlowRunnerTaskExecutorSupplier flowRunnerTaskExecutorSupplier, ConfigurableApplicationContext context,
			ReleaserProperties releaserProperties, BuildReportHandler reportHandler) {
		return new SpringBatchFlowRunner(jobRepository, manager, projectsToRunFactory, jobLauncher,
				flowRunnerTaskExecutorSupplier, context, releaserProperties, reportHandler);
	}

	@Bean
	Jackson2ExecutionContextStringSerializer myJackson2ExecutionContextStringSerializer() {
		Jackson2ExecutionContextStringSerializer serializer = new Jackson2ExecutionContextStringSerializer();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
		// Needed to add this to serialize the exceptions
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		objectMapper.enableDefaultTyping();
		serializer.setObjectMapper(objectMapper);
		return serializer;
	}
	/*
	 * // Needed to add this to serialize the exceptions
	 *
	 * @Bean BatchConfigurer myBatchConfigurer(DataSource dataSource,
	 * Jackson2ExecutionContextStringSerializer
	 * myJackson2ExecutionContextStringSerializer, PlatformTransactionManager
	 * transactionManager) { return new DefaultBatchConfigurer(dataSource) {
	 *
	 * @Override protected JobExplorer createJobExplorer() throws Exception {
	 * JobExplorerFactoryBean jobExplorerFactoryBean = new JobExplorerFactoryBean();
	 * jobExplorerFactoryBean.setDataSource(dataSource);
	 * jobExplorerFactoryBean.setSerializer(myJackson2ExecutionContextStringSerializer);
	 * jobExplorerFactoryBean.afterPropertiesSet(); return
	 * jobExplorerFactoryBean.getObject(); }
	 *
	 * @Override protected JobRepository createJobRepository() throws Exception {
	 * JobRepositoryFactoryBean jobRepositoryFactoryBean = new JobRepositoryFactoryBean();
	 * jobRepositoryFactoryBean.setDataSource(dataSource);
	 * jobRepositoryFactoryBean.setSerializer(myJackson2ExecutionContextStringSerializer);
	 * jobRepositoryFactoryBean.setTransactionManager(transactionManager);
	 * jobRepositoryFactoryBean.afterPropertiesSet(); return
	 * jobRepositoryFactoryBean.getObject(); } }; }
	 */

}
