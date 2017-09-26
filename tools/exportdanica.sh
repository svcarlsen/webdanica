# replace path in TOOLS_HOME with the correct full path
TOOLS_HOME=/REPLACE/WITH/CORRECT/FULL/PATH
ME=`basename $0`

if [ ! -f "$TOOLS_HOME" ]; then
  echo ERROR: The TOOLS_HOME \"$TOOLS_HOME\" does not exist. Please correct the path in $ME
  exit 1
fi

SETTINGSFILE=$TOOLS_HOME/conf/webdanica_settings.xml 
OPTS2=-Dwebdanica.settings.file=$SETTINGSFILE
OPTS3=-Dlogback.configurationFile=$TOOLS_HOME/conf/silent_logback.xml 

if [ ! -f "$SETTINGSFILE" ]; then
   echo "The webdanica settingsfile \'$SETTINGSFILE\' does not exist. Exiting program $ME"
   exit 1
fi

NAS_VERSION=5.2.2
VERSION=2.0
PHOENIX_JAR=lib/phoenix-4.7.0-HBase-1.1-client.jar
#PHOENIX_JAR=/usr/hdp/current/phoenix-client/phoenix-client.jar
WEBDANICA_JAR=lib/webdanica-core-$VERSION.jar

if [ ! -f "$WEBDANICA_JAR" ]; then
   echo "The WEBDANICA_JAR \'$WEBDANICA_JAR\' does not exist. Exiting program $ME"
   exit 1
fi

if [ ! -f "$PHOENIX_JAR" ]; then
   echo "The PHOENIX_JAR \'$PHOENIX_JAR\' does not exist. Exiting program $ME"
   exit 1
fi

java $OPTS2 $OPTS3 -cp $WEBDANICA_JAR:$PHOENIX_JAR:lib/common-core-$NAS_VERSION.jar:lib/harvester-core-$NAS_VERSION.jar dk.kb.webdanica.core.tools.ExportFromWebdanica --list_already_exported

