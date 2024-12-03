Spring Cloud Edgware builds on Spring Boot 1.5.x.
# Renamed starters

A number of starters did not follow normal Spring Cloud naming conventions. In Edgware, use of the deprecated starter will log a warning with the name of the new starter to use in its place. Below is a table of the deprecated starters and their replacements

|  Deprecated | Edgware Starter | 
| ------------- | ------------- | 
| spring-cloud-starter-archaius | spring-cloud-starter-netflix-archaius | 
| spring-cloud-starter-atlas | spring-cloud-starter-netflix-atlas | 
| spring-cloud-starter-eureka | spring-cloud-starter-netflix-eureka-client | 
| spring-cloud-starter-eureka-server | spring-cloud-starter-netflix-eureka-server | 
| spring-cloud-starter-feign | spring-cloud-starter-openfeign | 
| spring-cloud-starter-hystrix | spring-cloud-starter-netflix-hystrix | 
| spring-cloud-starter-hystrix-dashboard | spring-cloud-starter-netflix-hystrix-dashboard | 
| spring-cloud-starter-ribbon | spring-cloud-starter-netflix-ribbon | 
| spring-cloud-starter-spectator | spring-cloud-starter-netflix-spectator | 
| spring-cloud-starter-turbine | spring-cloud-starter-netflix-turbine | 
| spring-cloud-starter-turbine-amqp  | DELETED | 
| spring-cloud-starter-turbine-stream | spring-cloud-starter-netflix-turbine-stream | 
| spring-cloud-starter-zuul | spring-cloud-starter-netflix-zuul | 



# Edgware.SR6

2019-05-23

 - Spring Cloud Starter `Edgware.SR6` 
 - Spring Cloud Release `Edgware.SR6` 
 - Spring Cloud Security `1.2.4.RELEASE` 
 - Spring Cloud Bus `1.3.5.RELEASE` 
 - Spring Cloud Stream `Ditmars.SR5` 
 - Spring Cloud Task `1.2.4.RELEASE` 
 - Spring Cloud Netflix `1.4.7.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-netflix/issues?q=is%3Aclosed+milestone%3A1.4.7.RELEASE))
 - Spring Cloud Sleuth `1.3.6.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-sleuth/issues?q=is%3Aclosed+milestone%3A1.3.6.RELEASE))
 - Spring Cloud Config `1.4.7.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-config/milestone/56?closed=1))
 - Spring Cloud `Edgware.SR6` 
 - Spring Cloud Dependencies `Edgware.SR6` 
 - Spring Cloud Commons `1.3.6.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-commons/milestone/56?closed=1))
 - Spring Cloud Build `1.3.13.RELEASE` 
 - Spring Cloud Vault `1.1.3.RELEASE` (upgraded to Vault 1.1.3)
 - Spring Cloud Zookeeper `1.2.3.RELEASE` 
 - Spring Cloud Contract `1.2.7.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-contract/issues?q=is%3Aclosed+milestone%3A1.2.7.RELEASE))
 - Spring Cloud Aws `1.2.4.RELEASE` 
 - Spring Cloud Consul `1.3.6.RELEASE` 
 - Spring Cloud Cloudfoundry `1.1.3.RELEASE` 
 - Spring Cloud Function `1.0.2.RELEASE` 
 - Spring Cloud Gateway `1.0.3.RELEASE` 


# Edgware.SR5
2018-10-16

 - Spring Cloud Commons `1.3.5.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-commons/issues?q=is%3Aclosed+milestone%3A1.3.5.RELEASE))
 - Spring Cloud Config `1.4.5.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-config/issues?q=is%3Aclosed+milestone%3A1.4.5.RELEASE))
 - Spring Cloud Netflix `1.4.6.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-netflix/issues?q=is%3Aclosed+milestone%3A1.4.6.RELEASE))
 - Spring Cloud Sleuth `1.3.5.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-sleuth/issues?q=is%3Aclosed+milestone%3A1.3.5.RELEASE))
 - Spring Cloud Contract `1.2.6.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-contract/issues?q=is%3Aclosed+milestone%3A1.2.6.RELEASE))
 - Spring Cloud Vault `1.1.2.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-vault/issues?q=is%3Aclosed+milestone%3A1.1.2))
 - Spring Cloud Consul `1.3.5.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-consul/issues?q=is%3Aclosed+milestone%3A1.3.5.RELEASE))

# Edgware.SR4
2018-06-29

**spring-boot-autoconfigure-processor** - All of the projects below have a new optional dependency, `spring-boot-autoconfigure-processor`. See Spring Cloud Commons issue [#377](https://github.com/spring-cloud/spring-cloud-commons/issues/377).

 - Spring Cloud Commons `1.3.4.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-commons/issues?q=is%3Aclosed+milestone%3A1.3.4.RELEASE))
 - Spring Cloud AWS `1.2.3.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-aws/milestone/23?closed=1))
 - Spring Cloud Bus `1.3.4.RELEASE`
 - Spring Cloud Config `1.4.4.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-config/issues?q=is%3Aclosed+milestone%3A1.4.4.RELEASE))
 - Spring Cloud Netflix `1.4.5.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-netflix/issues?q=is%3Aclosed+milestone%3A1.4.5.RELEASE))
 - Spring Cloud Security `1.2.3.RELEASE`
 - Spring Cloud Consul `1.3.4.RELEASE`
 - Spring Cloud Zookeeper `1.2.2.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-zookeeper/issues?q=is%3Aclosed+milestone%3A1.2.2.RELEASE))
 - Spring Cloud Sleuth `1.3.4.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-sleuth/issues?q=is%3Aclosed+milestone%3A1.3.4.RELEASE))
 - Spring Cloud CloudFoundry `1.1.2.RELEASE`
 - Spring Cloud Contract `1.2.5.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-contract/issues?q=is%3Aclosed+milestone%3A1.2.5.RELEASE))
 - Spring Cloud Task `1.2.3.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-task/issues?q=is%3Aclosed+milestone%3A1.2.3.RELEASE))
 - Spring Cloud Vault `1.1.1.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-vault/issues?q=is%3Aclosed+milestone%3A1.1.1))
 - Spring Cloud Gateway `1.0.1.RELEASE`
 - Spring Cloud Function `1.0.0.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-function/milestone/7?closed=1))


# Edgware.SR3

2018-03-27

 - Spring Cloud Zookeeper `1.2.1.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-zookeeper/issues?q=is%3Aclosed+milestone%3A1.2.1.RELEASE))
 - Spring Cloud Config `1.4.3.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-config/milestone/45?closed=1))
 - Spring Cloud Commons `1.3.3.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-commons/milestone/45?closed=1)) 
 - Spring Cloud Sleuth `1.3.3.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-sleuth/issues?q=is%3Aclosed+milestone%3A1.3.3.RELEASE))
 - Spring Cloud Contract `1.2.4.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-contract/issues?q=is%3Aclosed+milestone%3A1.2.4.RELEASE))
 - Spring Cloud Netflix `1.4.4.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-netflix/issues?q=is%3Aclosed+milestone%3A1.4.4.RELEASE))
 - Spring Cloud Consul `1.3.3.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-consul/issues?q=is%3Aclosed+milestone%3A1.3.3.RELEASE))

# Edgware.SR2

2018-02-09
 - Spring Cloud Bus `1.3.3.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-bus/milestone/26?closed=1))
 - Spring Cloud Config `1.4.2.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-config/milestone/43?closed=1))
 - Spring Cloud Commons `1.3.2.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-commons/milestone/41?closed=1)) 
 - Spring Cloud Sleuth `1.3.2.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-sleuth/milestone/41?closed=1))
 - Spring Cloud Contract `1.2.3.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-contract/milestone/29?closed=1))
 - Spring Cloud Netflix `1.4.3.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-netflix/milestone/57?closed=1))
 - Spring Cloud Consul `1.3.2.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-consul/milestone/27?closed=1))

# Edgware.SR1

2018-01-16

 - Spring Cloud Config `1.4.1.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-config/milestone/40?closed=1))
 - Spring Cloud Commons `1.3.1.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-commons/milestone/39?closed=1))
 - Spring Cloud Stream `Ditmars.SR3` 
 - Spring Cloud Sleuth `1.3.1.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-sleuth/milestone/40?closed=1))
 - Spring Cloud Gateway `1.0.1.RELEASE` 
 - Spring Cloud Contract `1.2.2.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-contract/milestone/27?closed=1))
 - Spring Cloud Security `1.2.2.RELEASE` 
 - Spring Cloud Netflix `1.4.2.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-netflix/milestone/54?closed=1))
 - Spring Cloud Consul `1.3.1.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-consul/milestone/26?closed=1))

# Edgware.RELEASE

2017-11-27

 - Spring Cloud Config `1.4.0.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-config/milestone/39?closed=1))
 - Spring Cloud Task `1.2.2.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-task/milestone/19?closed=1))
 - Spring Cloud Commons `1.3.0.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-commons/milestone/36?closed=1))
 - Spring Cloud Stream `Ditmars.RELEASE` 
 - Spring Cloud Zookeeper `1.2.0.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-zookeeper/milestone/17?closed=1))
 - Spring Cloud Sleuth `1.3.0.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-sleuth/milestone/36?closed=1))
 - Spring Cloud Gateway `1.0.0.RELEASE` 
 - Spring Cloud Cloudfoundry `1.1.0.RELEASE` 
 - Spring Cloud Contract `1.2.0.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-contract/milestone/24?closed=1))
 - Spring Cloud Security `1.2.1.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-security/milestone/16?closed=1))
 - Spring Cloud Aws `1.2.2.RELEASE` 
 - Spring Cloud Vault `1.1.0.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-vault/milestone/13?closed=1))
 - Spring Cloud Netflix `1.4.0.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-netflix/milestone/52?closed=1))
 - Spring Cloud Bus `1.3.2.RELEASE` 
 - Spring Cloud Consul `1.3.0.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-consul/milestone/22?closed=1))

# Edgware.RC1

2017-10-24
 - Spring Cloud Bus `1.3.2.RC1` ([issues](https://github.com/spring-cloud/spring-cloud-bus/milestone/22?closed=1))
 - Spring Cloud Task `1.2.2.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-task/milestone/19?closed=1))
 - Spring Cloud Netflix `1.4.0.RC1` ([issues](https://github.com/spring-cloud/spring-cloud-netflix/milestone/48?closed=1))
 - Spring Cloud Consul `1.3.0.RC1` ([issues](https://github.com/spring-cloud/spring-cloud-consul/milestone/19?closed=1))
 - Spring Cloud Contract `1.2.0.RC1` ([issues](https://github.com/spring-cloud/spring-cloud-contract/milestone/20?closed=1))
 - Spring Cloud Sleuth `1.3.0.RC1` ([issues](https://github.com/spring-cloud/spring-cloud-sleuth/milestone/33?closed=1))
 - Spring Cloud Stream `Ditmars.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-stream-starters/releases/tag/vDitmars.RELEASE))
 - Spring Cloud Dependencies `1.3.5.RELEASE`
 - Spring Cloud Aws `1.2.2.RC1` ([issues](https://github.com/spring-cloud/spring-cloud-aws/milestone/18?closed=1))
 - Spring Cloud Config `1.4.0.RC1` ([issues](https://github.com/spring-cloud/spring-cloud-config/milestone/37?closed=1))
 - Spring Cloud Zookeeper `1.2.0.RC1` ([issues](https://github.com/spring-cloud/spring-cloud-zookeeper/milestone/15?closed=1))
 - Spring Cloud Gateway `1.0.0.RC1`
 - Spring Cloud Cloudfoundry `1.1.0.RELEASE`
 - Spring Cloud Commons `1.3.0.RC1` ([issues](https://github.com/spring-cloud/spring-cloud-commons/milestone/32?closed=1))
 - Spring Cloud Build `1.3.5.RELEASE`
 - Spring Cloud Security `1.2.1.RELEASE` ([issues](https://github.com/spring-cloud/spring-cloud-security/milestone/16?closed=1))
 - Spring Cloud Vault `1.1.0.RC1` ([issues](https://github.com/spring-cloud/spring-cloud-vault/milestone/10?closed=1))


# Edgware.M1

2017-08-29

- Spring Cloud Commons   `1.3.0.M1` ([issues](https://github.com/spring-cloud/spring-cloud-commons/milestone/29?closed=1))
- Spring Cloud Stream    `Ditmars.M2` ([issues](https://github.com/spring-cloud/spring-cloud-stream-starters/releases/tag/vDitmars.M2))
- Spring Cloud Config    `1.4.0.M1` ([issues](https://github.com/spring-cloud/spring-cloud-config/milestone/36?closed=1))
- Spring Cloud Contract  `1.2.0.M1` ([issues](https://github.com/spring-cloud/spring-cloud-contract/milestone/13?closed=1))
- Spring Cloud Netflix   `1.4.0.M1` ([issues](https://github.com/spring-cloud/spring-cloud-netflix/milestone/45?closed=1))
- Spring Cloud Zookeeper `1.2.0.M1` ([issues](https://github.com/spring-cloud/spring-cloud-zookeeper/milestone/14?closed=1))
- Spring Cloud Sleuth    `1.3.0.M1` ([issues](https://github.com/spring-cloud/spring-cloud-sleuth/milestone/29?closed=1))
- Spring Cloud Vault     `1.1.0.M1` ([issues](https://github.com/spring-cloud/spring-cloud-vault/milestone/5?closed=1))
- Spring Cloud Gateway   `1.0.0.M1` ([issues](https://github.com/spring-cloud/spring-cloud-gateway/milestone/2?closed=1))

