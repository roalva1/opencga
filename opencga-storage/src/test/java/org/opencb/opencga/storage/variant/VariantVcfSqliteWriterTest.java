package org.opencb.opencga.storage.variant;

import org.apache.tools.ant.util.FileUtils;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.opencb.commons.bioformats.variant.Variant;
import org.opencb.commons.bioformats.variant.VariantStudy;
import org.opencb.commons.bioformats.variant.json.VariantInfo;
import org.opencb.commons.bioformats.variant.vcf4.io.readers.VariantReader;
import org.opencb.commons.bioformats.variant.vcf4.io.readers.VariantVcfReader;
import org.opencb.commons.bioformats.variant.vcf4.io.writers.VariantWriter;
import org.opencb.commons.containers.list.SortedList;
import org.opencb.commons.db.SqliteSingletonConnection;
import org.opencb.commons.run.Task;
import org.opencb.commons.test.GenericTest;
import org.opencb.opencga.lib.auth.IllegalOpenCGACredentialsException;
import org.opencb.opencga.lib.auth.SqliteCredentials;
import org.opencb.opencga.lib.common.XObject;
import org.opencb.opencga.storage.indices.SqliteManager;
import org.opencb.variant.lib.runners.VariantRunner;
import org.opencb.variant.lib.runners.tasks.VariantEffectTask;
import org.opencb.variant.lib.runners.tasks.VariantStatsTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Alejandro Aleman Ramos <aaleman@cipf.es>
 */
@FixMethodOrder(MethodSorters.JVM)
public class VariantVcfSqliteWriterTest extends GenericTest {


    private static String oldDBName = "/tmp/old.db";
    private static String newDBName = "/tmp/new.db";
    //    private static String inputFile = VariantVcfSqliteWriterTest.class.getResource("/variant-test-file.vcf.gz").getFile();
    private static String inputFile = "/home/aaleman/Documents/pruebas/index/aux.vcf";

    private static List<Task<Variant>> newTaskList;
    private static List<Task<Variant>> oldTaskList;
    private static VariantStudy study;

    private static Properties oldProperties;
    private static Properties newProperties;

    private SqliteManager sqmOld = new SqliteManager();
    private SqliteManager sqmNew = new SqliteManager();


    @BeforeClass
    public static void init() throws IllegalOpenCGACredentialsException, IOException {
        study = new VariantStudy("test", "test", "test", null, null);
        newTaskList = new SortedList<>();
        oldTaskList = new SortedList<>();
        FileUtils.delete(new File(oldDBName));
        FileUtils.delete(new File(newDBName));

        oldProperties = new Properties();
        oldProperties.put("db_path", oldDBName);

        newProperties = new Properties();
        newProperties.put("db_path", newDBName);


        SqliteSingletonConnection oldSq = new SqliteSingletonConnection(oldDBName);


        VariantReader oldReader = new VariantVcfReader(inputFile);
        VariantWriter oldWriter = new VariantVcfSqliteWriter(new SqliteCredentials(oldProperties));
        oldWriter.includeSamples(true);
        oldWriter.includeEffect(true);
        oldTaskList.add(new VariantStatsTask(oldReader, study));
        oldTaskList.add(new VariantEffectTask());

        VariantRunner oldRunner = new VariantRunner(study, oldReader, null, Arrays.asList(oldWriter), oldTaskList);

        long startOld = System.currentTimeMillis();

        oldRunner.run();
        System.out.println("Index Time (old); " + (System.currentTimeMillis() - startOld));

        SqliteSingletonConnection.closeConnection();


        SqliteSingletonConnection newSq = new SqliteSingletonConnection(newDBName);

        VariantReader newReader = new VariantVcfReader(inputFile);
        VariantWriter newWriter = new VariantVcfNewSqliteWriter(new SqliteCredentials(newProperties));
        newWriter.includeSamples(true);
        newWriter.includeStats(true);
        newWriter.includeEffect(true);
        newTaskList.add(new VariantStatsTask(newReader, study));
        newTaskList.add(new VariantEffectTask());

        VariantRunner newRunner = new VariantRunner(study, newReader, null, Arrays.asList(newWriter), newTaskList);

        long startNew = System.currentTimeMillis();
        newRunner.run();
        System.out.println("Index Time (new); " + (System.currentTimeMillis() - startNew));

    }


    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        sqmOld.connect(Paths.get(oldDBName), true);
        sqmNew.connect(Paths.get(oldDBName), true);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        sqmOld.disconnect(false);
        sqmNew.disconnect(false);
        super.tearDown();
    }


    @Test
    public void testSelectVariantOld() throws Exception {

        List<XObject> res;
        res = sqmOld.query("select * from variant;");

    }

    @Test
    public void testSelectVariantNew() throws Exception {

        List<XObject> res;
        res = sqmNew.query("select * from variant;");

    }

    @Test
    public void testSelectVariantStatsOld() throws Exception {

        List<XObject> res;
        res = sqmOld.query("SELECT * FROM variant_stats inner join variant on variant_stats.chromosome=variant.chromosome AND variant_stats.position=variant.position AND variant_stats.allele_ref=variant.ref AND variant_stats.allele_alt=variant.alt ;");

    }

    @Test
    public void testSelectVariantStatsNew() throws Exception {

        List<XObject> res;
        res = sqmNew.query("SELECT * FROM variant inner join variant_stats on variant.id_variant=variant_stats.id_variant;");

    }

    @Test
    public void testSelectVariantStatsInfoOld() throws Exception {

        List<XObject> res;
        res = sqmOld.query("SELECT * FROM variant_stats " +
                "inner join variant on variant_stats.chromosome=variant.chromosome AND variant_stats.position=variant.position AND " +
                "variant_stats.allele_ref=variant.ref AND variant_stats.allele_alt=variant.alt " +
                "left join variant_info on variant.id_variant=variant_info.id_variant " +
                ";");

    }

    @Test
    public void testSelectVariantStatsInfoNew() throws Exception {

        List<XObject> res;
        res = sqmNew.query("SELECT * FROM variant " +
                "inner join variant_stats on variant.id_variant=variant_stats.id_variant " +
                "left join variant_info on variant.id_variant=variant_info.id_variant;");

    }

    @Test
    public void testSelectVariantStatsInfoSamplesOld() throws Exception {

        List<XObject> res;
        res = sqmOld.query("SELECT * FROM variant_stats " +
                "inner join variant on variant_stats.chromosome=variant.chromosome AND variant_stats.position=variant.position AND " +
                "variant_stats.allele_ref=variant.ref AND variant_stats.allele_alt=variant.alt " +
                "left join variant_info on variant.id_variant=variant_info.id_variant " +
                ";");

    }

    @Test
    public void testSelectVariantStatsInfoSamplesNew() throws Exception {

        List<XObject> res;
        res = sqmNew.query("SELECT * FROM variant " +
                "inner join variant_stats on variant.id_variant=variant_stats.id_variant " +
                "left join variant_info on variant.id_variant=variant_info.id_variant;");

    }


}
