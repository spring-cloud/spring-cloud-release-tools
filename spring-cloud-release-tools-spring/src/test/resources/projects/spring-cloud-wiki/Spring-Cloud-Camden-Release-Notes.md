Spring Cloud Camden builds on Spring Boot 1.4.x.


# Camden.SR7

* Spring Cloud Netflix `1.2.7.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-netflix/milestone/39?closed=1)
* Spring Cloud Sleuth `1.1.4.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-sleuth/milestone/24?closed=1) Contains bug fixes, added an option to adjust spans before reporting them, fixed async support
* Spring Cloud Contract `1.0.5.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-contract/milestone/11?closed=1). Contains bug fixes, documentation update, consumer stub co-dependency feature
* Spring Cloud AWS `1.1.4.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-aws/milestone/15?closed=1)
* Spring Cloud Security `1.1.4.RELEASE` Contains documentation fixes.
* Spring Cloud Consul `1.1.4.RELEASE` Excludes Spring Core from Spring Retry dependency.



# Camden.SR6

* Spring Cloud Commons `1.1.8.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-commons/milestone/21?closed=1)
* Spring Cloud Stream `Brooklyn.SR3` [(issue)](https://github.com/spring-cloud/spring-cloud-stream/issues/810)
* Spring Cloud Bus `1.2.2` [(issues)](https://github.com/spring-cloud/spring-cloud-bus/milestone/19?closed=1)
* Spring Cloud Config `1.2.3.RELEASE` [(issue)](https://github.com/spring-cloud/spring-cloud-config/issues/638)
* Spring Cloud Netflix `1.2.6.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-netflix/milestone/38?closed=1)
* Spring Cloud Consul `1.1.3.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-consul/milestone/14?closed=1)
* Spring Cloud Zookeeper `1.0.4.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-zookeeper/milestone/8?closed=1) 
* Spring Cloud Sleuth `1.1.3.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-sleuth/milestone/22?closed=1)
* Spring Cloud Contract `1.0.4.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-contract/milestone/9?closed=1)

# Camden.SR5
Adds Boot 1.5 compatibility and breaks Boot 1.3 compatibility

See [this gist](https://gist.github.com/spencergibb/80a134b4738c78ca64e4d0c77214fcb6) for a workaround for "MIME type may not contain reserved characters" errors using Zuul URL mappings with mime-types with character encoding.

* 2017/02/03 - Spring Cloud Build `1.2.2.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-build/milestone/12?closed=1)
* Spring Cloud Stream `Brooklyn.SR2`[(blog post)](https://spring.io/blog/2017/01/20/spring-cloud-stream-brooklyn-sr2-and-chelsea-m1-released)
* Spring Cloud Netflix `1.2.5.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-netflix/milestone/36?closed=1)
* Spring Cloud Sleuth `1.1.2.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-sleuth/milestone/21?closed=1) 

# Camden.SR4
* 2017/01/09 - Spring Cloud Commons `1.1.7.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-commons/milestone/21?closed=1)
* 2017/01/11 - Spring Cloud Netflix `1.2.4.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-netflix/milestone/35?closed=1)
* 2017/01/12 - Spring Cloud Sleuth `1.1.1.RELEASE`. The [(1.0.x issues)](https://github.com/spring-cloud/spring-cloud-sleuth/milestone/19?closed=1) and [(1.1.x issues)](https://github.com/spring-cloud/spring-cloud-sleuth/milestone/20?closed=1). All features merged to 1.0.x are also present in 1.1.x. Major changes:
  * Continues spans instead of creating new ones (this change was required to happen due to issues with async communication and view controller based one) [#474](https://github.com/spring-cloud/spring-cloud-sleuth/pulls/474)
  * Added `spring.instance_id` tag to know which server exactly the span originates from [#488](https://github.com/spring-cloud/spring-cloud-sleuth/pull/488)
  * Support for non-web apps got fixed [#32](https://github.com/spring-cloud/spring-cloud-sleuth/issues/32)
  * Parent-ID is added to MDC so you can reference it in the logs if you want to [#480](https://github.com/spring-cloud/spring-cloud-sleuth/issues/480)
* 2017/01/12 - Spring Cloud Contract `1.0.3.RELEASE`. The [(issues)](https://github.com/spring-cloud/spring-cloud-contract/milestone/8?closed=1). Major changes:
  * Added `stubMatchers` and `testMatchers` to the DSL [#185](https://github.com/spring-cloud/spring-cloud-contract/issues/185)
  * Fixed the Explicit mode (thanks to this you can work with context paths properly) [#179](https://github.com/spring-cloud/spring-cloud-contract/issues/179) [#117](https://github.com/spring-cloud/spring-cloud-contract/issues/117)
  * Added environment property for every dependency [#147](https://github.com/spring-cloud/spring-cloud-contract/issues/147)

# Camden.SR3
* 2016/11/23 - Spring Cloud Commons `1.1.6.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-commons/milestone/20?closed=1)
* 2016/11/23 - Spring Cloud Config `1.2.2.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-config/milestone/26?closed=1)
* 2016/11/23 - Spring Cloud Netflix `1.2.3.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-netflix/milestone/33?closed=1)
* 2016/11/28 - Spring Cloud Consul `1.1.2.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-consul/milestone/13?closed=1)
* 2016/11/24 - Spring Cloud Contract `1.0.2.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-contract/milestone/7?closed=1)
* 2016/11/24 - Spring Cloud Sleuth `1.1.0.RELEASE`. The [(issues)](https://github.com/spring-cloud/spring-cloud-sleuth/milestone/17?closed=1) are taken from `1.0.11.RELEASE` since `1.1.x` should differ from `1.0.x` only by dependencies. The features should be the same.

# Camden.SR2
* 2016/11/01 - Spring Cloud Netflix `1.2.2.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-netflix/milestone/32?closed=1)
* 2016/10/18 - Spring Cloud Consul `1.1.1.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-consul/milestone/12?closed=1) 
* 2016/11/01 - Spring Cloud CLI `1.2.2.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-cli/milestone/12?closed=1)
* 2016/10/31 - Spring Cloud Streams `Brooklyn.SR1` [(issues)](https://github.com/spring-cloud/spring-cloud-stream-binder-rabbit/milestone/5?closed=1)


# Camden.SR1

- 2016/09/21 - AWS version `1.1.3.RELEASE`
- 2016/09/21 - Bus version `1.2.1.RELEASE`
- 2016/10/17 - Commons version `1.1.4.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-commons/milestone/19?closed=1)
- 2016/10/17 - Contract version `1.0.1.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-contract/milestone/6?closed=1)
- 2016/10/17 - Config version `1.2.1.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-config/milestone/25?closed=1)
- 2016/10/17 - Netflix version `1.2.1.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-netflix/milestone/29?closed=1)
- 2016/09/06 - Security version `1.1.3.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-security/milestone/13?closed=1)
- 2016/10/17 - Sleuth version `1.0.10.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-sleuth/milestone/16?closed=1)
- 2016/09/21 - Stream version `Brooklyn.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-stream-starters/wiki/Brooklyn-Release-Notes)
- 2016/09/08 - Task version `1.0.3.RELEASE`
- 2016/09/21 - Zookeeper version `1.0.3.RELEASE`

# Camden.RELEASE

## New projects

It adds the following new projects:

### Spring Cloud Contract

- 2016/09/23 - Updated to version `1.0.0.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-contract/milestone/5?closed=1)
- 2016/09/14 - Updated to version `1.0.0.RC1` [(issues)](https://github.com/spring-cloud/spring-cloud-contract/milestone/3?closed=1)
- 2016/08/29 - Released version `1.0.0.M2` ([M1 issues](https://github.com/spring-cloud/spring-cloud-contract/milestone/1?closed=1), [M2 issues](https://github.com/spring-cloud/spring-cloud-contract/milestone/2?closed=1))

What you always need is confidence in pushing new features into a new application or service in a distributed system. This project provides support for Consumer Driven Contracts and service schemas in Spring applications, covering a range of options for writing tests, publishing them as assets, asserting that a contract is kept by producers and consumers, for HTTP and message-based interactions.

The migration guide between milestones is available [here](https://github.com/spring-cloud/spring-cloud-contract/wiki/1.0.0-Migration-Guides)

## Existing projects

It has the following changes from existing applications

### Spring Cloud Stream

- 2016/09/21 - Updated to version `Brooklyn.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-stream/milestone/16?closed=1)
- 2016/09/14 - Updated to version `Brooklyn.RC1` [(issues)](https://github.com/spring-cloud/spring-cloud-stream/milestone/12?closed=1)
- 2016/08/29 - Updated to version `Brooklyn.M1`

You can check out the detailed [release notes here](https://github.com/spring-cloud/spring-cloud-stream-starters/wiki/Brooklyn-Release-Notes)

### Spring Cloud Bus

- 2016/09/22 - Updated to version `1.2.0.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-bus/milestone/17?closed=1)
- 2016/09/14 - Updated to version `1.2.0.RC1` [(issues)](https://github.com/spring-cloud/spring-cloud-bus/milestone/16?closed=1)
- 2016/08/29 - Updated to version `1.2.0.M1` [(issues)](https://github.com/spring-cloud/spring-cloud-bus/milestone/14?closed=1).

### Spring Cloud Config

- 2016/09/22 - Updated to version `1.2.0.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-config/milestone/24?closed=1)
- 2016/09/14 - Updated to version `1.2.0.RC1` [(issues)](https://github.com/spring-cloud/spring-cloud-config/milestone/23?closed=1)
 2016/08/29 - Updated to version `1.2.0.M1` [(issues)](https://github.com/spring-cloud/spring-cloud-config/milestone/20?closed=1).

Notable changes:

- Documentation updates
- Placeholder resolution fixes


### Spring Cloud Netflix

- 2016/09/22 - Updated to version `1.2.0.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-netflix/milestone/28?closed=1)
- 2016/09/14 - Updated to version `1.2.0.RC1` [(issues)](https://github.com/spring-cloud/spring-cloud-netflix/milestone/27?closed=1)
- 2016/08/29 - Updated to version `1.2.0.M1` [(issues)](https://github.com/spring-cloud/spring-cloud-netflix/milestone/22?closed=1)

Notable changes:

- Customize Ribbon component classes using properties
- Feign upgrade (moves to community maintained openfeign)
- Zuul fixes

Note: There was an XXE vulnerability in xstream (which is used by Eureka), so please upgrade to the Camden release train to pull in the latest version of that library which fixed the issue (https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2016-3674).

### Spring Cloud Consul

- 2016/09/22 - Updated to version `1.1.0.RELEASE` [(issues)](https://github.com/spring-cloud/spring-cloud-consul/milestone/11?closed=1)
- 2016/09/14 - Updated to version `1.1.0.RC1` [(issues)](https://github.com/spring-cloud/spring-cloud-consul/milestone/10?closed=1)
- 2016/08/29 - Updated to version `1.1.0.M1` [(issues)](https://github.com/spring-cloud/spring-cloud-consul/milestone/9?closed=1)

Notable changes:

- Documentation updates
- Consul Bus rewritten as a Stream Binder

### Spring Cloud CLI

- 2016/09/26 - Updated to version `1.2.0.RC1`
[(issues)](https://github.com/spring-cloud/spring-cloud-cli/milestone/9?closed=1)

Notable changes:

- Created launcher (via `spring cloud` cli command)
