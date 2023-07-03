# Spring Cloud Info

## Why?

The Spring Cloud Team gets a lot of inquiries from users like the following:

* What version of Spring Cloud should I use if I am using Spring Boot `x.x.x`?
* When will Spring Cloud `[RELEASE]` be released?
* What version of `spring-cloud-*` is in Spring Cloud `[RELEASE]`?

The answers to these questions can easily be found if you know where to look.  The purpose of
Spring Cloud Info is to make these answers easily accessible via a REST API that we can then use
to expose this information in a user friendly way.

## The Nitty Gritty Details

Spring Cloud Info is deployed on Pivotal Cloud Foundry in the org `spring.io` and space `
spring-cloud-issuebot-production`.

There is a `manifest.yml` file in the root of the project you can use to deploy the app
simply by running `cf push`.  It requires a Spring Cloud Services Config Server service 
named `config-server`.  The config server should point to the configuration in the branch
`spring-cloud-info-config` of the repo https://github.com/spring-cloud/spring-cloud-release-tools.
Within the `application.yml` in that branch there needs to be one an encrypted oauth token for GitHub
that is set via `spring.cloud.info.git.oauthtoken`.  This token is used by Spring Cloud Info to fetch
data from GitHub.

## REST API
The rest API is documented via Spring Rest Docs and is published along with the app to Pivotal
Cloud Foundry.  You can find the REST API documentation at 
https://spring-cloud-info.apps.pcfone.io/docs/spring-cloud-info.html.
