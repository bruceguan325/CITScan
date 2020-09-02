export WSIMPORT="$JAVA_HOME/bin/wsimport -s ../src -Xnocompile"
$WSIMPORT -p tw.com.cathaybk.webservice.client.bancs BANCSService.xml
$WSIMPORT -p tw.com.cathaybk.webservice.client.card CARDService.xml
$WSIMPORT -p tw.com.cathaybk.webservice.client.fep FEPService.xml