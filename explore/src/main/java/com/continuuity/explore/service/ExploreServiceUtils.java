package com.continuuity.explore.service;

import com.continuuity.common.conf.Constants;
import com.continuuity.data2.datafabric.dataset.service.DatasetService;
import com.continuuity.data2.util.hbase.HBaseTableUtilFactory;
import com.continuuity.explore.guice.ExploreRuntimeModule;
import com.continuuity.explore.service.hive.Hive12ExploreService;
import com.continuuity.explore.service.hive.Hive13ExploreService;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.apache.hadoop.util.VersionInfo;
import org.apache.twill.internal.utils.Dependencies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility class for the explore service.
 */
public class ExploreServiceUtils {
  private static final Logger LOG = LoggerFactory.getLogger(ExploreServiceUtils.class);
  /**
   * Hive support enum.
   */
  public enum HiveSupport {
    // todo populate this with whatever hive version CDH4.3 runs with - REACTOR-229
    HIVE_CDH4(Pattern.compile("^.*cdh4\\..*$"), Hive12ExploreService.class),
    HIVE_12(Pattern.compile("^.*2\\.2\\.0.*$"), Hive12ExploreService.class),
    HIVE_13(Pattern.compile("^.*$"), Hive13ExploreService.class);

    private final Pattern hadoopVersionPattern;
    private final Class hiveExploreServiceClass;

    private HiveSupport(Pattern hadoopVersionPattern, Class hiveExploreServiceClass) {
      this.hadoopVersionPattern = hadoopVersionPattern;
      this.hiveExploreServiceClass = hiveExploreServiceClass;
    }

    public Pattern getHadoopVersionPattern() {
      return hadoopVersionPattern;
    }

    public Class getHiveExploreServiceClass() {
      return hiveExploreServiceClass;
    }
  }

  // Caching the dependencies so that we don't trace them twice
  private static Set<File> exploreDependencies = null;
  // Caching explore class loader
  private static ClassLoader exploreClassLoader = null;

  /**
   * Get all the files contained in a class path.
   */
  public static Iterable<File> getClassPathJarsFiles(String hiveClassPath) {
    if (hiveClassPath == null) {
      return null;
    }
    return Iterables.transform(Splitter.on(':').split(hiveClassPath), STRING_FILE_FUNCTION);
  }

  private static final Function<String, File> STRING_FILE_FUNCTION =
    new Function<String, File>() {
      @Override
      public File apply(String input) {
        return new File(input).getAbsoluteFile();
      }
    };

  /**
   * Builds a class loader with the class path provided.
   */
  public static ClassLoader getExploreClassLoader() {
    if (exploreClassLoader != null) {
      return exploreClassLoader;
    }

    // HIVE_CLASSPATH will be defined in startup scripts if Hive is installed.
    String exploreClassPathStr = System.getProperty(Constants.Explore.HIVE_CLASSPATH);
    LOG.debug("Explore classpath = {}", exploreClassPathStr);
    if (exploreClassPathStr == null) {
      throw new RuntimeException("System property " + Constants.Explore.HIVE_CLASSPATH + " is not set.");
    }

    Iterable<File> hiveClassPath = getClassPathJarsFiles(exploreClassPathStr);
    ImmutableList.Builder<URL> builder = ImmutableList.builder();
    for (File jar : hiveClassPath) {
      try {
        builder.add(jar.toURI().toURL());
      } catch (MalformedURLException e) {
        LOG.error("Jar URL is malformed", e);
        Throwables.propagate(e);
      }
    }
    exploreClassLoader = new URLClassLoader(Iterables.toArray(builder.build(), URL.class),
                                            ClassLoader.getSystemClassLoader());
    return exploreClassLoader;
  }

  public static Class getHiveService() {
    HiveSupport hiveVersion = checkHiveSupport(null);
    Class hiveServiceCl = hiveVersion.getHiveExploreServiceClass();
    return hiveServiceCl;
  }

  /**
   * Check that Hive is in the class path - with a right version. Use a separate class loader to load Hive classes,
   * built using the explore classpath passed as a system property to reactor-master.
   */
  public static HiveSupport checkHiveSupport() {
    ClassLoader classLoader = getExploreClassLoader();
    return checkHiveSupport(classLoader);
  }

  /**
   * Check that Hive is in the class path - with a right version.
   *
   * @param hiveClassLoader class loader to use to load hive classes.
   *                        If null, the class loader of this class is used.
   */
  public static HiveSupport checkHiveSupport(ClassLoader hiveClassLoader) {
    try {
      ClassLoader usingCL = hiveClassLoader;
      if (usingCL == null) {
        usingCL = ExploreServiceUtils.class.getClassLoader();
      }

      // In Hive 12, CLIService.getOperationStatus returns OperationState.
      // In Hive 13, CLIService.getOperationStatus returns OperationStatus.
//      Class cliServiceClass = usingCL.loadClass("org.apache.hive.service.cli.CLIService");
//      Class operationHandleCl = usingCL.loadClass("org.apache.hive.service.cli.OperationHandle");
//      Method getStatusMethod = cliServiceClass.getDeclaredMethod("getOperationStatus", operationHandleCl);

      // Rowset is an interface in Hive 13, but a class in Hive 12
//      Class rowSetClass = usingCL.loadClass("org.apache.hive.service.cli.RowSet");

      String hadoopVersion = VersionInfo.getVersion();
      LOG.info("Hadoop version is: {}", hadoopVersion);
      for (HiveSupport hiveSupport : HiveSupport.values()) {
        if (hiveSupport.getHadoopVersionPattern().matcher(hadoopVersion).matches()) {
          return hiveSupport;
        }
      }

      // TODO What about distributions that don't have hive installed by default, but still have hive installed?
      // Like our loom distributions?

//      if (rowSetClass.isInterface()
//        && getStatusMethod.getReturnType() == usingCL.loadClass("org.apache.hive.service.cli.OperationStatus")) {
//        return HiveSupport.HIVE_13;
//      } else if (!rowSetClass.isInterface()
//        && getStatusMethod.getReturnType() == usingCL.loadClass("org.apache.hive.service.cli.OperationState")) {
//        return HiveSupport.HIVE_12;
//      }
      throw new RuntimeException("Hive distribution not supported. Set the configuration reactor.explore.enabled " +
                                 "to false to start up Reactor without Explore.");
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException("Hive jars not present in classpath. Set the configuration reactor.explore.enabled " +
                                 "to false to start up Reactor without Explore.", e);
    }
  }

  /**
   * Return the list of absolute paths of the bootstrap classes.
   */
  public static Set<String> getBoostrapClasses() {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    for (String classpath : Splitter.on(File.pathSeparatorChar).split(System.getProperty("sun.boot.class.path"))) {
      File file = new File(classpath);
      builder.add(file.getAbsolutePath());
      try {
        builder.add(file.getCanonicalPath());
      } catch (IOException e) {
        LOG.warn("Could not add canonical path to aux class path for file {}", file.toString(), e);
      }
    }
    return builder.build();
  }

  /**
   * Trace the jar dependencies needed by the Explore container. Uses a separate class loader to load Hive classes,
   * built using the explore classpath passed as a system property to reactor-master.
   *
   * @return an ordered set of jar files.
   */
  public static Set<File> traceExploreDependencies() throws IOException {
    if (exploreDependencies != null) {
      return exploreDependencies;
    }

    ClassLoader classLoader = getExploreClassLoader();
    return traceExploreDependencies(classLoader);
  }

  /**
   * Trace the jar dependencies needed by the Explore container.
   *
   * @param classLoader class loader to use to trace the dependencies.
   *                    If it is null, use the class loader of this class.
   * @return an ordered set of jar files.
   */
  public static Set<File> traceExploreDependencies(ClassLoader classLoader)
    throws IOException {
    if (exploreDependencies != null) {
      return exploreDependencies;
    }

    ClassLoader usingCL = classLoader;
    if (classLoader == null) {
      usingCL = ExploreRuntimeModule.class.getClassLoader();
    }
    Set<String> bootstrapClassPaths = getBoostrapClasses();

    Set<File> hBaseTableDeps = traceDependencies(new HBaseTableUtilFactory().get().getClass().getCanonicalName(),
                                                 bootstrapClassPaths, usingCL);

    // Note the order of dependency jars is important so that HBase jars come first in the classpath order
    // LinkedHashSet maintains insertion order while removing duplicate entries.
    Set<File> orderedDependencies = new LinkedHashSet<File>();
    orderedDependencies.addAll(hBaseTableDeps);
    orderedDependencies.addAll(traceDependencies(DatasetService.class.getCanonicalName(),
                                                 bootstrapClassPaths, usingCL));
    orderedDependencies.addAll(traceDependencies("com.continuuity.hive.datasets.DatasetStorageHandler",
                                                 bootstrapClassPaths, usingCL));
    orderedDependencies.addAll(traceDependencies("org.apache.hadoop.hive.ql.exec.mr.ExecDriver",
                                                 bootstrapClassPaths, usingCL));
    orderedDependencies.addAll(traceDependencies("org.apache.hive.service.cli.CLIService",
                                                 bootstrapClassPaths, usingCL));
    orderedDependencies.addAll(traceDependencies("org.apache.hadoop.mapred.YarnClientProtocolProvider",
                                                 bootstrapClassPaths, usingCL));

    // Needed for - at least - CDH 4.4 integration
    orderedDependencies.addAll(traceDependencies("org.apache.hive.builtins.BuiltinUtils",
                                                 bootstrapClassPaths, usingCL));

    // Needed for - at least - CDH 5 integration
    orderedDependencies.addAll(
      traceDependencies("org.apache.hadoop.yarn.server.resourcemanager.scheduler.fair.FairScheduler",
                        bootstrapClassPaths, usingCL));

    exploreDependencies = orderedDependencies;
    return orderedDependencies;
  }

  /**
   * Trace the dependencies files of the given className, using the classLoader, and excluding any class contained in
   * the bootstrapClassPaths.
   */
  public static Set<File> traceDependencies(String className, final Set<String> bootstrapClassPaths,
                                            ClassLoader classLoader)
    throws IOException {
    ClassLoader usingCL = classLoader;
    if (usingCL == null) {
      usingCL = ExploreRuntimeModule.class.getClassLoader();
    }
    final Set<File> jarFiles = Sets.newHashSet();

    Dependencies.findClassDependencies(
      usingCL,
      new Dependencies.ClassAcceptor() {
        @Override
        public boolean accept(String className, URL classUrl, URL classPathUrl) {
          if (bootstrapClassPaths.contains(classPathUrl.getFile())) {
            return false;
          }

          jarFiles.add(new File(classPathUrl.getFile()));
          return true;
        }
      },
      className
    );

    return jarFiles;
  }


}
