@echo off
java -Xms128m -Xmx768m -classpath .;dq-client.jar;lib/mina.jar;lib/xpp3.jar;lib/slf4j.jar;lib/xstream.jar;lib/mysql-connector.jar;lib/hex-string.jar;lib/jmf.jar;lib/defs.jar org.darkquest.client.mudclient
pause
