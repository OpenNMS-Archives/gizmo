Gizmo
=====

End-to-end testing with JUnit and Docker.

Getting Started
---------------

Add *gizmo* to your Maven dependencies:

```xml
<dependency>
    <groupId>org.opennms.gizmo</groupId>
    <artifactId>gizmo-docker</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

Use JUnit rules to configure one or more *stacks*:

```java
@Rule
public GizmoDockerRule gizmo = GizmoDockerRule.builder()
    .withContainer(NGINX_ALIAS, (stacker) -> {
       return ContainerConfig.builder()
               .image("nginx:1.11.1-alpine")
               .hostConfig(HostConfig.builder()
                .publishAllPorts(true)
                .build())
               .build();
    })
    .withWaitingRule((stacker) -> {
        // When the container ports are bound to random host ports
        // you can use this call to determine the effective address of the service
        final InetSocketAddress httpAddr = stacker.getServiceAddress(NGINX_ALIAS, 80);
        await().atMost(2, MINUTES).pollInterval(5, SECONDS).pollDelay(0, SECONDS)
            .until(() -> HttpUtils.get(httpAddr, "/") != null);
    }).build();
```

Reference the services hosted by the *stacks* in your tests:

```java
@Test
public void canSpawnContainerAndDoGet() throws IOException {
    // At this point, our container should is up and running and ready to answer requests
    final GizmoDockerStacker stacker = gizmo.getStacker();
    final InetSocketAddress httpAddr = stacker.getServiceAddress(NGINX_ALIAS, 80);
    assertThat(HttpUtils.get(httpAddr, "/"), containsString("Welcome to nginx!"));
}
```

Take a look at [the integration tests][1] for more examples.

 [1]: https://github.com/j-white/gizmo/tree/master/docker/src/test/java/org/opennms/gizmo/docker



