# Thank you for your interest in this plugin. Please visit the official Pulumi [plugin repository](https://github.com/pulumi/spinnaker-preconfigured-job-plugin) instead.

-------

# pulumi-plugin

https://www.pulumi.com/
Pulumi plugin is a custom pipeline stage. This plugins enables a Stage on Orca that allow you to run Pulumi code stored on your github repository.

The plugin consists of a Armory.PulumiPlugin Kotlin server component that uses the Pulumi cli tool to run the commands.

This is for demo only and not meant to be used in a production environment.

Note: Currently this plugin is able to create infrastructure in AWS using typescript code and poiting to a github repository.

#Usage
Run ./gradlew releaseBundle
Put the /build/distributions/<project>-<version>.zip into the configured plugins location for your service.
Configure the Spinnaker service. Put the following in the service yml to enable the plugin and configure the extension:

```
spinnaker:
  extensibility:
    plugins:
      Armory.PulumiPlugin:
        enabled: true
        extensions:
          armory.pulumiPlugin:
            enabled: true
            config:
              AWS_ACCESS_KEY_ID:
              AWS_SECRET_ACCESS_KEY:
```
