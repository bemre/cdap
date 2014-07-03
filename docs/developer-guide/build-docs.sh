#!/usr/bin/env bash

# Build script for Reactor docs
# Builds the docs
# Copies the javadocs into place
# Zips everything up so it can be staged

DATE_STAMP=`date`
SCRIPT=`basename $0`

SOURCE="source"
BUILD="build"
BUILD_PDF="build-pdf"
HTML="html"
APIDOCS="apidocs"
JAVADOCS="javadocs"
LICENSES="licenses"
LICENSES_PDF="licenses-pdf"
EXAMPLES="examples"

EXAMPLE_PVA="PageViewAnalytics"
EXAMPLE_RCA="ResponseCodeAnalytics"
EXAMPLE_TA="TrafficAnalytics"

SCRIPT_PATH=`pwd`
SOURCE_PATH="$SCRIPT_PATH/$SOURCE"
BUILD_PATH="$SCRIPT_PATH/$BUILD"
HTML_PATH="$BUILD_PATH/$HTML"

DOCS_PY="$SCRIPT_PATH/../tools/scripts/docs.py"

REST_SOURCE="$SOURCE_PATH/rest.rst"
REST_PDF="$SCRIPT_PATH/$BUILD_PDF/rest.pdf"

INSTALL_GUIDE="$SCRIPT_PATH/../install-guide"
INSTALL_SOURCE="$INSTALL_GUIDE/source/install.rst"


VERSION_TXT="version.txt"

if [ "x$2" == "x" ]; then
  REACTOR_PATH="$SCRIPT_PATH/../../"
else
  REACTOR_PATH="$2"
fi
REACTOR_JAVADOCS="$REACTOR_PATH/continuuity-api/target/site/apidocs"

ZIP_FILE_NAME=$HTML
ZIP_FILE="$ZIP_FILE_NAME.zip"
STAGING_SERVER="stg-web101.sw.joyent.continuuity.net"

function usage() {
  cd $REACTOR_PATH
  REACTOR_PATH=`pwd`
  echo "Build script for Reactor docs"
  echo "Usage: $SCRIPT < option > [reactor]"
  echo ""
  echo "  Options (select one)"
  echo "    build        Clean build of javadocs, docs (HTML and PDF), copy javadocs and pdfs, zip results"
  echo "    stage        Stages and logins to server"
  echo "  or "
  echo "    build-docs   Clean build of docs"
  echo "    javadocs     Clean build of javadocs"
  echo "    pdf-rest     Clean build of REST PDF"
  echo "    pdf-install  Clean build of Install Guide PDF"
  echo "    login        Logs you into $STAGING_SERVER"
  echo "    reactor      Path to Reactor source for javadocs, if not $REACTOR_PATH"
  echo "    zip          Zips docs into $ZIP_FILE"
  echo "  or "
  echo "    depends      Build Site listing dependencies"  
  echo "    sdk          Build SDK"  
  echo " "
  exit 1
}

function clean() {
  rm -rf $SCRIPT_PATH/$BUILD
}

function build_javadocs() {
  cd $REACTOR_PATH
  mvn clean package site -pl continuuity-api -am -Pjavadocs -DskipTests
}

function build_docs() {
  clean
  sphinx-build -b html -d build/doctrees source build/html
}

function build_pdf_rest() {
  version
  rm -rf $SCRIPT_PATH/$BUILD_PDF
  mkdir $SCRIPT_PATH/$BUILD_PDF
  python $DOCS_PY -g pdf -o $REST_PDF $REST_SOURCE
}

function build_pdf_install() {
  version
  INSTALL_PDF="$INSTALL_GUIDE/$BUILD_PDF/Reactor-Installation-Guide-v$reactor_version.pdf"
  rm -rf $INSTALL_GUIDE/$BUILD_PDF
  mkdir $INSTALL_GUIDE/$BUILD_PDF
  python $DOCS_PY -g pdf -o $INSTALL_PDF $INSTALL_SOURCE
}

function copy_javadocs() {
  cd $BUILD_PATH/$HTML
  rm -rf $JAVADOCS
  cp -r $REACTOR_JAVADOCS .
  mv -f $APIDOCS $JAVADOCS
}

function copy_license_pdfs() {
  cd $BUILD_PATH/$HTML/$LICENSES
  cp $SCRIPT_PATH/$LICENSES_PDF/* .
}

function make_zip() {
  cd $SCRIPT_PATH/$BUILD
  zip -r $ZIP_FILE_NAME $HTML/*
}

function stage_docs() {
  echo "Deploying..."
  echo "rsync -vz $SCRIPT_PATH/$BUILD/$ZIP_FILE \"$USER@$STAGING_SERVER:$ZIP_FILE\""
  rsync -vz $SCRIPT_PATH/$BUILD/$ZIP_FILE "$USER@$STAGING_SERVER:$ZIP_FILE"
  version
  echo ""
  echo "To install on server:"
  echo "cd /var/www/reactor; ls"
  echo "sudo rm -rf $reactor_version; ls"
  echo "sudo unzip ~/$ZIP_FILE; sudo mv $HTML $reactor_version"
  echo "or"
  echo "cd /var/www/reactor; ls; sudo rm -rf $reactor_version; sudo unzip ~/$ZIP_FILE; sudo mv $HTML $reactor_version"
  echo ""
  login_staging_server
}

function login_staging_server() {
  echo "Logging into:"
  echo "ssh \"$USER@$STAGING_SERVER\""
  ssh "$USER@$STAGING_SERVER"
}

function build() {
   build_docs
   build_javadocs
   build_pdf_rest
   build_pdf_install
   copy_javadocs
   copy_license_pdfs
   make_zip
}

function build_sdk() {
  cd $REACTOR_PATH
  mvn clean package -DskipTests -P examples && mvn package -pl singlenode -am -DskipTests -P dist,release
}

function build_dependencies() {
  cd $REACTOR_PATH
  mvn clean package site -am -Pjavadocs -DskipTests
}

function version() {
#   cd $REACTOR_PATH
#   reactor_version=$(cat $VERSION_TXT)
   reactor_version=$(cat $REACTOR_PATH/$VERSION_TXT)
   echo "Reactor version: $reactor_version"
}

if [ $# -lt 1 ]; then
  usage
  exit 1
fi

case "$1" in
  build )             build; exit 1;;
  build-javadocs )    build_javadocs; exit 1;;
  build-docs )        build_docs; exit 1;;
  copy_javadocs )     copy_javadocs; exit 1;;
  copy_license_pdfs ) copy_license_pdfs; exit 1;;
  javadocs )          build_javadocs; exit 1;;
  depends )           build_dependencies; exit 1;;
  login )             login_staging_server; exit 1;;
  pdf-install )       build_pdf_install; exit 1;;
  pdf-rest )          build_pdf_rest; exit 1;;
  sdk )               build_sdk; exit 1;;
  stage )             stage_docs; exit 1;;
  version )           version; exit 1;;
  zip )               make_zip; exit 1;;
  * )                 usage; exit 1;;
esac
