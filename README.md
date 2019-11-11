# PCM-Headless
This repository is based on the previous PCM-Docker project ([PCM-Docker Repository](https://github.com/dmonsch/PCM-Docker)) and is in all aspects superior compared to the "old" implementation. Therefore you should avoid using the PCM-Docker repository if possible.

# Features
This implementation provides fully headless simulations of PCM (Palladio Component Model) instances. You can either use the code directly in your project as dependency (the harder way), or setup the provided Docker image and easily use the exposed REST interface.

Supported simulation engines:
- [x] SimuCom
- [x] SimuLizar
- [ ] EventSim (not planned but maybe some future stuff)

If you want to know more about how the headless simulations work you can click [here](https://github.com/dmonsch/PCM-Headless/wiki/How-it-works).

# Setup
In general, there are two different ways to use the headless implementation. The first and simpler method is the use of the prepared Docker Image in combination with the offered REST interface. The second possibility is the direct use of the Gradle projects and the source code.

1. [Setup using Docker](https://github.com/dmonsch/PCM-Headless/wiki/Setup-using-Docker)
2. [Setup using Gradle](https://github.com/dmonsch/PCM-Headless/wiki/Setup-using-Gradle-project)

Or the easy way, just use Docker and execute: `docker run --name pcm-docker -p 8080:8080 davmonsch/pcm-headless-rest`<br>
After that you are ready to go.

# Usage
The usage is very intuitive and in general looks like this:

```java
PCMHeadlessClient client = new PCMHeadlessClient("http://127.0.0.1:8080/");
if (client.isReachable(3000)) {
	// new client for a new simulation
	SimulationClient sim = client.prepareSimulation();
	sim.setAllocation(allocationFile);
	sim.setRepository(repositoryFile);
	sim.setSystem(systemFile);
	sim.setUsageModel(usageFile);
	sim.setResourceEnvironment(resourceEnvironmentFile);
	sim.setMonitorRepository(monitorRepositoryFile);
	
	// set default config
	sim.setSimulationConfig(HeadlessSimulationConfig.builder().build());
	
	// transitive closure of models & transfer to REST
	sim.createTransitiveClosure();
	sim.sync();
	
	// simulate and use results
	sim.executeSimulation(res -> {
		// do something with the results
	});
}
```

More examples and explanations can be found under [Usage](https://github.com/dmonsch/PCM-Headless/wiki/Usage).

# Web UI
The Spring-Boot application that provides the REST interface also offers a very simple Web UI. With the help of this UI it is possible to view the status of the current simulations. Furthermore it offers the possibility to trigger simulations manually.

![Screenshot of the Web UI](https://user-images.githubusercontent.com/19149680/68165136-e2e24880-ff5e-11e9-8f93-e03b5f63ad14.png)

# Future Work
The current limitations, problems and future improvements are documented [here](https://github.com/dmonsch/PCM-Headless/issues).

# Questions?
Look into the [Wiki](https://github.com/dmonsch/PCM-Headless/wiki) and see if it addresses your question(s) or feel free to open an issue.

# Credits
This repository is based on many other projects. The most important of them are:
* [Palladio Component Model](https://sdqweb.ipd.kit.edu/wiki/Palladio_Component_Model) and several related projects
* [Eclipse Modeling Framework](Eclipse Modeling Framework)
* [Docker](https://www.docker.com/)
* [Spring Boot](https://spring.io/projects/spring-boot)
* [Lombok](https://projectlombok.org/)