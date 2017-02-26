# mr-collector
mapreduce collector

# Usage

$ YARN_CLIENT_OPTS=-javaagent:/path/to/this/jar/file
$ yarn jar jarFile [mainClass] args...

or

$ HADOOP_CLIENT_OPTS=-javaagent:/path/to/this/jar/file
$ hadoop jar jarFile [mainClass] args...
