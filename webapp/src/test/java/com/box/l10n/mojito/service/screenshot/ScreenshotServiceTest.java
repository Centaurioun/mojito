package com.box.l10n.mojito.service.screenshot;

import static com.box.l10n.mojito.entity.Screenshot.Status.ACCEPTED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.*;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.thirdparty.ThirdPartyScreenshotRepository;
import com.box.l10n.mojito.service.thirdparty.ThirdPartyService;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJobConfig;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJobsConfig;
import com.box.l10n.mojito.service.tm.TMTestData;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.*;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/** @author jeanaurambault */
public class ScreenshotServiceTest extends ServiceTestBase {

  /** logger */
  static Logger logger = getLogger(ScreenshotServiceTest.class);

  @Autowired RepositoryService repositoryService;

  @Autowired ScreenshotService screenshotService;

  @Autowired LocaleService localeService;

  @Autowired ScreenshotRunRepository screenshotRunRepository;

  @Autowired ScreenshotRepository screenshotRepository;

  @Autowired RepositoryRepository repositoryRepository;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Mock ThirdPartyService thirdPartyServiceMock;

  @Autowired ThirdPartyService thirdPartyService;

  @Autowired ThirdPartyScreenshotRepository thirdPartyScreenshotRepository;

  @Autowired ScreenshotTextUnitRepository screenshotTextUnitRepository;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired AssetService assetService;

  @Autowired ThirdPartySyncJobsConfig thirdPartySyncJobsConfig;

  @Test
  public void testDeleteScreenshot() throws Exception {
    final Map<String, ThirdPartySyncJobConfig> thirdPartySyncJobs =
        thirdPartySyncJobsConfig.getThirdPartySyncJobs();

    screenshotService.thirdPartyService = thirdPartyServiceMock;

    Screenshot screenshot = new Screenshot();
    screenshot.setId(1L);
    screenshot.setName("screenshot");
    screenshotRepository.save(screenshot);

    Assert.assertNotNull(screenshotRepository.findById(screenshot.getId()).orElse(null));

    ThirdPartyScreenshot thirdPartyScreenshot = new ThirdPartyScreenshot();
    thirdPartyScreenshot.setScreenshot(screenshot);
    thirdPartyScreenshot.setId(1L);
    thirdPartyScreenshot.setThirdPartyId("smartlingScreenshotId");
    thirdPartyScreenshotRepository.save(thirdPartyScreenshot);

    List<ThirdPartyScreenshot> createdThirdPartyScreenshots =
        thirdPartyScreenshotRepository.findAllByScreenshotId(screenshot.getId());
    Assert.assertEquals(createdThirdPartyScreenshots.get(0).getId(), thirdPartyScreenshot.getId());

    Repository repository = repositoryService.createRepository("testRepository");

    Asset asset =
        assetService.createAssetWithContent(repository.getId(), "fake_for_test", "fake for test");

    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setAsset(asset);
    tmTextUnitRepository.save(tmTextUnit);

    ScreenshotTextUnit screenshotTextUnit = new ScreenshotTextUnit();
    screenshotTextUnit.setId(1L);
    screenshotTextUnit.setScreenshot(screenshot);
    screenshotTextUnit.setTmTextUnit(tmTextUnit);
    screenshotTextUnitRepository.save(screenshotTextUnit);

    ScreenshotTextUnit createdScreenshotTextUnit =
        screenshotTextUnitRepository.findById(screenshotTextUnit.getId()).orElse(null);
    assertEquals(createdScreenshotTextUnit.getScreenshot().getId(), screenshot.getId());

    screenshotService.deleteScreenshot(screenshot.getId());

    verify(thirdPartyServiceMock, times(1))
        .removeImage(
            thirdPartySyncJobs.get(repository.getName()).getThirdPartyProjectId(),
            thirdPartyScreenshot.getThirdPartyId());
    Assert.assertNull(screenshotRepository.findById(screenshot.getId()).orElse(null));
    Assert.assertEquals(
        0, thirdPartyScreenshotRepository.findAllByScreenshotId(screenshot.getId()).size());
    Assert.assertNull(
        screenshotTextUnitRepository.findById(screenshotTextUnit.getId()).orElse(null));
  }

  @Test
  public void testCreateScreenshotRun() throws Exception {

    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    ScreenshotRun screenshotRun = new ScreenshotRun();
    screenshotRun.setName(UUID.randomUUID().toString());
    screenshotRun.setRepository(repository);

    Screenshot screenshot1 = new Screenshot();
    screenshot1.setName("s1");
    Screenshot screenshot2 = new Screenshot();
    screenshot2.setName("s2");
    screenshotRun.getScreenshots().add(screenshot1);
    screenshotRun.getScreenshots().add(screenshot2);

    ScreenshotRun createScreenshotRun =
        screenshotService.createOrAddToScreenshotRun(screenshotRun, true);

    ScreenshotRun createdFromDB =
        screenshotRunRepository.findById(createScreenshotRun.getId()).orElse(null);
    ArrayList<Screenshot> arrayList = new ArrayList<>(createdFromDB.getScreenshots());
    Assert.assertNotNull(arrayList.get(0).getId());
    Assert.assertNotNull(arrayList.get(1).getId());
  }

  @Test
  public void testManualRun() throws Exception {

    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    ScreenshotRun screenshotRun = new ScreenshotRun();
    screenshotRun.setName(UUID.randomUUID().toString());
    screenshotRun.setRepository(repository);

    Screenshot screenshot1 = new Screenshot();
    screenshot1.setName("screen1");
    screenshot1.setStatus(ACCEPTED);
    screenshot1.setLocale(repository.getSourceLocale());

    Screenshot screenshot2 = new Screenshot();
    screenshot2.setName("screen2");
    screenshot2.setStatus(ACCEPTED);
    screenshot2.setLocale(repository.getSourceLocale());

    screenshotRun.getScreenshots().add(screenshot1);
    screenshotRun.getScreenshots().add(screenshot2);

    ScreenshotRun createScreenshotRun =
        screenshotService.createOrAddToScreenshotRun(screenshotRun, false);

    repository.setManualScreenshotRun(createScreenshotRun);
    repositoryRepository.save(repository);

    List<Screenshot> searchScreenshotsNoManualRuns =
        screenshotService.searchScreenshots(
            Arrays.asList(repository.getId()),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            ScreenshotRunType.LAST_SUCCESSFUL_RUN,
            0,
            10);

    assertTrue(searchScreenshotsNoManualRuns.isEmpty());

    List<Screenshot> searchScreenshotsManualRuns =
        screenshotService.searchScreenshots(
            Arrays.asList(repository.getId()),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            ScreenshotRunType.MANUAL_RUN,
            0,
            10);
    assertEquals("screen1", searchScreenshotsManualRuns.get(0).getName());
    assertEquals("screen2", searchScreenshotsManualRuns.get(1).getName());
  }

  @Test
  public void testUpdateScreenshotRun() throws Exception {

    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    ScreenshotRun screenshotRun = new ScreenshotRun();
    screenshotRun.setRepository(repository);
    screenshotRun.setName(UUID.randomUUID().toString());

    Screenshot screenshot1 = new Screenshot();
    screenshot1.setName("s1");
    Screenshot screenshot2 = new Screenshot();
    screenshot2.setName("s2");
    screenshotRun.getScreenshots().add(screenshot1);
    screenshotRun.getScreenshots().add(screenshot2);
    screenshotService.createOrAddToScreenshotRun(screenshotRun, true);

    ScreenshotRun forUpdate = new ScreenshotRun();
    forUpdate.setRepository(repository);
    forUpdate.setName(screenshotRun.getName());

    Screenshot screenshot3 = new Screenshot();
    screenshot3.setName("s3");
    forUpdate.getScreenshots().add(screenshot3);

    ScreenshotRun updatedScreenshotRun =
        screenshotService.createOrAddToScreenshotRun(forUpdate, true);

    ScreenshotRun updatedFromDB =
        screenshotRunRepository.findById(updatedScreenshotRun.getId()).orElse(null);
    ArrayList<Screenshot> arrayList = new ArrayList<>(updatedFromDB.getScreenshots());
    Assert.assertNotNull(arrayList.get(0).getId());
    Assert.assertNotNull(arrayList.get(1).getId());
    Assert.assertNotNull(arrayList.get(2).getId());
  }

  @Test
  public void testSortScreenshotBySequence() {
    List<Screenshot> screenshots = new ArrayList<>();

    Screenshot screen1 = new Screenshot();
    screen1.setSequence(1L);
    screenshots.add(screen1);

    Screenshot screen2 = new Screenshot();
    screen2.setSequence(10L);
    screenshots.add(screen2);

    Screenshot screen3 = new Screenshot();
    screen3.setSequence(5L);
    screenshots.add(screen3);

    screenshotService.sortScreenshotBySequence(screenshots);

    assertEquals(screen1, screenshots.get(0));
    assertEquals(screen3, screenshots.get(1));
    assertEquals(screen2, screenshots.get(2));
  }

  @Test
  public void testSearchScreenshotsByRepository() throws RepositoryNameAlreadyUsedException {
    Repository repository = createScreenshotDataAsRepository();
    List<Screenshot> searchScreenshots =
        screenshotService.searchScreenshots(
            Arrays.asList(repository.getId()),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            ScreenshotRunType.LAST_SUCCESSFUL_RUN,
            0,
            10);
    assertEquals("screen1", searchScreenshots.get(0).getName());
    assertEquals("screen3", searchScreenshots.get(1).getName());
    assertEquals("screen2", searchScreenshots.get(2).getName());
  }

  @Test
  public void testSearchScreenshotsByLocale() throws RepositoryNameAlreadyUsedException {

    Repository repository = createScreenshotDataAsRepository();

    List<Screenshot> searchScreenshots =
        screenshotService.searchScreenshots(
            Arrays.asList(repository.getId()),
            Arrays.asList("fr-FR"),
            null,
            null,
            null,
            null,
            null,
            null,
            ScreenshotRunType.LAST_SUCCESSFUL_RUN,
            0,
            10);

    assertEquals("screen1", searchScreenshots.get(0).getName());
    assertEquals("screen2", searchScreenshots.get(1).getName());
    assertEquals(2, searchScreenshots.size());

    searchScreenshots =
        screenshotService.searchScreenshots(
            Arrays.asList(repository.getId()),
            Arrays.asList("ko-KR"),
            null,
            null,
            null,
            null,
            null,
            null,
            ScreenshotRunType.LAST_SUCCESSFUL_RUN,
            0,
            10);

    assertEquals("screen3", searchScreenshots.get(0).getName());
    assertEquals(1, searchScreenshots.size());
  }

  @Test
  public void testSearchScreenshotsByName() throws RepositoryNameAlreadyUsedException {

    Repository repository = createScreenshotDataAsRepository();

    List<Screenshot> searchScreenshots =
        screenshotService.searchScreenshots(
            Arrays.asList(repository.getId()),
            null,
            "screen2",
            null,
            null,
            null,
            null,
            null,
            ScreenshotRunType.LAST_SUCCESSFUL_RUN,
            0,
            10);

    assertEquals("screen2", searchScreenshots.get(0).getName());
    assertEquals(1, searchScreenshots.size());
  }

  @Test
  public void testSearchScreenshotsByStatus() throws RepositoryNameAlreadyUsedException {

    Repository repository = createScreenshotDataAsRepository();

    List<Screenshot> searchScreenshots =
        screenshotService.searchScreenshots(
            Arrays.asList(repository.getId()),
            null,
            null,
            Screenshot.Status.NEEDS_REVIEW,
            null,
            null,
            null,
            null,
            ScreenshotRunType.LAST_SUCCESSFUL_RUN,
            0,
            10);

    assertEquals("screen3", searchScreenshots.get(0).getName());
    assertEquals(1, searchScreenshots.size());

    searchScreenshots =
        screenshotService.searchScreenshots(
            Arrays.asList(repository.getId()),
            null,
            null,
            Screenshot.Status.REJECTED,
            null,
            null,
            null,
            null,
            ScreenshotRunType.LAST_SUCCESSFUL_RUN,
            0,
            10);

    assertEquals("screen1", searchScreenshots.get(0).getName());
    assertEquals(1, searchScreenshots.size());

    searchScreenshots =
        screenshotService.searchScreenshots(
            Arrays.asList(repository.getId()),
            null,
            null,
            ACCEPTED,
            null,
            null,
            null,
            null,
            ScreenshotRunType.LAST_SUCCESSFUL_RUN,
            0,
            10);

    assertEquals("screen2", searchScreenshots.get(0).getName());
    assertEquals(1, searchScreenshots.size());
  }

  @Test
  public void testSearchScreenshotsByPagination() throws RepositoryNameAlreadyUsedException {

    Repository repository = createScreenshotDataAsRepository();

    List<Screenshot> searchScreenshots =
        screenshotService.searchScreenshots(
            Arrays.asList(repository.getId()),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            ScreenshotRunType.LAST_SUCCESSFUL_RUN,
            1,
            1);

    logger.debug("repository: {}", repository.getId());
    assertEquals("screen3", searchScreenshots.get(0).getName());
    assertEquals(1, searchScreenshots.size());
  }

  @Test
  public void testLastRunSuccessful() throws RepositoryNameAlreadyUsedException {

    Repository repository = createScreenshotDataAsRepository();
    createScreenshotDataForRepository(repository);

    List<Screenshot> searchScreenshots =
        screenshotService.searchScreenshots(
            Arrays.asList(repository.getId()),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            ScreenshotRunType.LAST_SUCCESSFUL_RUN,
            0,
            10);

    assertEquals(3, searchScreenshots.size());
  }

  public Repository createScreenshotDataAsRepository() {
    return createScreenshotData().repository;
  }

  public TMTestData createScreenshotData() {
    TMTestData tmTestDataSource = new TMTestData(testIdWatcher);
    createScreenshotDataForRepository(tmTestDataSource.repository);
    return tmTestDataSource;
  }

  public void createScreenshotDataForRepository(Repository repository) {

    Screenshot screen1 = new Screenshot();
    screen1.setName("screen1");
    screen1.setLocale(localeService.findByBcp47Tag("fr-FR"));
    screen1.setSequence(1L);
    screen1.setStatus(Screenshot.Status.REJECTED);

    Screenshot screen2 = new Screenshot();
    screen2.setName("screen2");
    screen2.setLocale(localeService.findByBcp47Tag("fr-FR"));
    screen2.setSequence(10L);
    screen2.setStatus(ACCEPTED);

    Screenshot screen3 = new Screenshot();
    screen3.setName("screen3");
    screen3.setLocale(localeService.findByBcp47Tag("ko-KR"));
    screen3.setSequence(5L);
    screen3.setStatus(Screenshot.Status.NEEDS_REVIEW);

    ScreenshotRun screenshotRun = new ScreenshotRun();
    screenshotRun.setName(UUID.randomUUID().toString());
    screenshotRun.setRepository(repository);
    screenshotRun.getScreenshots().add(screen1);
    screenshotRun.getScreenshots().add(screen2);
    screenshotRun.getScreenshots().add(screen3);
    screenshotService.createOrAddToScreenshotRun(screenshotRun, true);
  }
}
