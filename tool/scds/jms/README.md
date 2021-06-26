# Consumer JumpStart
This is a simple JMS Consumer that allows for testing the connection to SCDS.
It is not intended to be used for a real world application.
The JumpStart allows you to log the message rate metrics and/or messages to the console.

### Configuration Options
The following configuration options will be provided when you create a subscription:
- **providerUrl**: url of the message broker including the port (e.g. tcps://hostname:55443)
- **queue**: the name of the queue to connect to receive data
- **connectionFactory**: the connection factory used for connecting which contains specific configuration parameters defined by an administrator
- **username**: the connection username for authentication
- **password**: the connection password for authentication
- **vpn**: the message vpn to connect to on the broker
- **metrics** (Optional): log message rate metrics to the console and defaults to `true`. Set it to `false` for disabling metrics.
- **output** (Optional): log messages to a specific output and defaults to `com.harris.cinnato.outputs.NoopOutput`.

#### Builtin Output Types
- **com.harris.cinnato.outputs.NoopOutput**: does not output message
- **com.harris.cinnato.outputs.StdoutOutput**: outputs the messages to standard out
- **com.harris.cinnato.outputs.FileOutput**: outputs the messages to a single rotating file log located in `./log/messages.log`
- **com.harris.cinnato.outputs.MessageFileOutput**: outputs each message to a separate file located in `./log/`

### Running with CLI options
```
./bin/run -DproviderUrl=url -Dqueue=queue -DconnectionFactory=factory -Dusername=user -Dpassword=pass -Dvpn=vpn
```

### Running with config file
```
./bin/run -Dconfig.file=path/to/config-file
```

Config File Example (application.conf)
```
providerUrl:
queue:
connectionFactory:
username:
password:
vpn:
metrics:false
output:com.harris.cinnato.outputs.FileOutput
json:true
```

### Create a new output
Extend the `com.harris.cinnato.outputs.Output` class and implement a `output(String message)` function.  When launching the JumpStart kit pass in new class `-Doutput=com.harris.cinnato.outputs.<NewClass>`
