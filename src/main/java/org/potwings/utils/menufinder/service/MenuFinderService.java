package org.potwings.utils.menufinder.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.potwings.utils.menufinder.dto.StoreInfo;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class MenuFinderService {

  public List<StoreInfo> findMenuHaveStore(String findStore, String findMenu) {

    System.setProperty("webdriver.chrome.driver", "C:/zzz/chromedriver-win64/chromedriver.exe");

    List<String> findMenuList = new ArrayList<>();
    if (findMenu.contains(",")) {
      // 메뉴가 여러개 들어가 있는 경우
      String[] findMenus = findMenu.split(",");
      for (String findMenuItem : findMenus) {
        findMenuList.add(findMenuItem.trim());
      }
    } else {
      findMenuList.add(findMenu);
    }

    ChromeOptions options = new ChromeOptions();
    options.addArguments("--start-maximized");
    options.addArguments("--remote-allow-origins=*");
    WebDriver driver = new ChromeDriver(options);
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    List<StoreInfo> resultList = null;
    try {
      driver.get("https://map.naver.com");  // 네이버 지도 접속

      // 1. 찾으려는 음식점 검색
      WebElement inputSearch = wait.until(
          ExpectedConditions.presenceOfElementLocated(By.className("input_search")));
      inputSearch.clear();
      inputSearch.sendKeys(findStore);
      inputSearch.sendKeys(Keys.ENTER);

      WebElement searchIframe = wait.until(
          ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#searchIframe")));
      driver.switchTo().frame(searchIframe); // 검색 결과 iframe으로 이동

      resultList = new ArrayList<>();
      // 음식점 목록 끝 페이지까지 반복하여 진행
      while (true) {

        // 모든 음식점 목록을 불러오기 위해 스크롤 진행
        // for test: 빠른 테스트를 위해 비활성화
        // TODO ul 하위에 lazyload-wrapper라는 클래스를 가진 div가 있는지 여부를 확인하도록 수정
        // scrollWholeList(driver, wait);

        // 키워드에 따라 HTML 요소가 다르게 노출됨으로 mainDiv 탐색 후 그 하위의 li를 통하여 store 목록 확인
        WebElement mainDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("Ryr1F")));

        // mainDiv 하위의 ul 태그에서 모든 li 태그를 가져옴
        List<WebElement> allLiElements = mainDiv.findElements(By.tagName("li"));

        List<WebElement> storeList;
        if (!allLiElements.isEmpty()) {
            // 첫 번째 li 태그의 클래스를 가져옴
            String firstLiClass = allLiElements.get(0).getAttribute("class");

            // 광고 li를 제외하기 위해 첫 번째 li 태그와 동일한 클래스를 가진 li만 필터링
            storeList = allLiElements.stream()
                .filter(li -> li.getAttribute("class").equals(firstLiClass))
                .toList();
        } else {
            storeList = new ArrayList<>(); // li 태그가 없으면 빈 리스트로 초기화
        }
        for (WebElement store : storeList) {
          // li 중 클릭하여 store 상세보기 할 수 있는 부분 탐색
          WebElement clickableStoreArea = store.findElement((By.tagName("a")));
          wait.until(ExpectedConditions.elementToBeClickable(clickableStoreArea)).click();

          // 음식점에 특정 메뉴 포함하고 있는지 여부 확인
          StoreInfo matchedStore = checkStoreMenus(driver, wait, findMenuList);
          if (matchedStore != null) {
            resultList.add(matchedStore);
          }
          driver.switchTo().frame(searchIframe); // 검색 iframe으로 이동
        }

        if (!resultList.isEmpty()) {
          // 메뉴를 가지고 있는 음식점을 하나라도 찾았을 경우 중단
          break;
        }

        // [이전 페이지, 다음 페이지] 버튼 목록 중 다음 페이지 선택
        WebElement nextPageBtn = driver.findElements(By.className("eUTV2")).get(1);
        boolean hasNextPage = nextPageBtn.getAttribute("aria-disabled").equals("false");
        if (!hasNextPage) {
          // 더 이상 확인할 다음 페이지가 없는 경우 중단
          break;
        }
        nextPageBtn.click(); // 다음 페이지로 이동
      }
    } catch (Exception e) {
      log.error("findMenu fail : {}", e.getMessage(), e);
    } finally {
      // for test: 이슈 발생 시 확인하기 위해 브라우저 종료하지 않고 유지
      // driver.quit();
    }
    return resultList;
  }

  /**
   * 무한 스크롤 방식에서 전체 목록을 불러오기 위해 목록 스크롤
   *
   * @param driver
   * @param wait
   * @throws InterruptedException
   */
  private void scrollWholeList(WebDriver driver, WebDriverWait wait) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    WebElement scrollDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.cssSelector("#_pcmap_list_scroll_container")));

    long scrollTop = 0;
    while (true) {
      // 컨텐츠 로딩 속도를 위해 스크롤 천천히 아래로 이동
      js.executeScript("arguments[0].scrollTop += arguments[1];", scrollDiv, 700);

      // 약간의 대기 시간 (렌더링 시간 고려)
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ie) {
        log.debug("Thread Sleep fail : {}", ie.getMessage(), ie);
      }

      long newScrollTop = (long) js.executeScript("return arguments[0].scrollTop", scrollDiv);
      if (scrollTop == newScrollTop) {
        break;
      }
      scrollTop = newScrollTop;
    }
  }

  private StoreInfo checkStoreMenus(WebDriver driver, WebDriverWait wait,
      List<String> findMenuList) {

    StoreInfo matchResult = null;
    try {
      driver.switchTo().defaultContent(); // 가게 정보 Iframe 찾을 수 있도록 전체 화면으로 focus 이동
      WebElement storeIframe = wait.until(
          ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#entryIframe")));
      driver.switchTo().frame(storeIframe); // 음식점 iframe으로 이동

      // 음식점 페이지 중단 탭 버튼
      List<WebElement> spans = driver.findElements(By.className("veBoZ"));
      boolean haveMenu = false;
      for (WebElement span : spans) {
        // 그 중 메뉴 버튼 클릭
        if (span.getText().equals("메뉴")) {
          span.click();
          haveMenu = true;
          break;
        }
      }

      if (!haveMenu) {
        // 메뉴가 등록되어있지 않은 경우 중단
        return null;
      }

      // 모든 메뉴 목록을 불러오기 위해 더보기 클릭
      WebElement moreMenu = null;
      // 메뉴가 하나라도 로딩될 때까지 대기
      List<WebElement> menuList = new ArrayList<>();
      boolean isNormalStoreInfo = false;

      try {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("lPzHi")));
        isNormalStoreInfo = true;
      } catch (TimeoutException e) {
        // lPzHi 태그에 메뉴를 가지고 있지 않은 다른 HTML 형식일 경우
        log.info("lPzHi class not found, checking for alternative menu format: {}", e.getMessage());

        // 다른 형식의 메뉴 요소 찾기 (예: 다른 클래스명 사용)
        try {
          wait.until(ExpectedConditions.visibilityOfElementLocated(
              By.className("MenuContent__tit__313LA")));
        } catch (TimeoutException ex) {
          log.info("No menu found in any format: {}", ex.getMessage());
          return null;
        }
      }

      // 메뉴 목록 추출
      if (isNormalStoreInfo) {
        // 일반적인 음식점 정보인 경우
        while (true) {
          try {
            moreMenu = driver.findElement(By.className("TeItc"));
          } catch (NoSuchElementException e) {
            break;
          }
          moreMenu.click();
          // TODO wait.until로 변경할 방안 도출
          Thread.sleep(3000L); // 로딩 대기
        }
        menuList = driver.findElements(By.className("lPzHi"));
      } else {
        // 네이버 플레이스를 통해 주문 가능한 음식점의 경우
        // 무한 스크롤 처리를 위해 페이지 끝까지 스크롤
        JavascriptExecutor js = (JavascriptExecutor) driver;
        long lastHeight = (long) js.executeScript("return document.body.scrollHeight");

        while (true) {
          // 현재 스크롤 위치 가져오기
          long currentPosition = (long) js.executeScript("return window.pageYOffset");
          long targetPosition = (long) js.executeScript("return document.body.scrollHeight");

          // 천천히 스크롤 (한 번에 300px씩)
          while (currentPosition < targetPosition - 100) {
            currentPosition += 300;
            js.executeScript("window.scrollTo(0, " + currentPosition + ");");

            // 각 스크롤 단계마다 짧은 대기
            try {
              Thread.sleep(200L);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              break;
            }
          }

          // 마지막으로 페이지 끝까지 스크롤
          js.executeScript("window.scrollTo(0, document.body.scrollHeight);");

          // 새로운 콘텐츠가 로드될 때까지 대기
          try {
            Thread.sleep(1000L);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }

          // 새로운 높이 확인
          long newHeight = (long) js.executeScript("return document.body.scrollHeight");

          // 더 이상 새로운 콘텐츠가 없으면 중단
          if (newHeight == lastHeight) {
            break;
          }
          lastHeight = newHeight;
        }

        // 모든 메뉴 목록 가져오기
        menuList = driver.findElements(By.className("MenuContent__tit__313LA"));
      }

      List<WebElement> findResult = menuList.stream()
          .filter(el -> {
            String text = el.getText();
            return findMenuList.stream().anyMatch(text::contains);
          })
          .toList();

      if (!findResult.isEmpty()) {
        // 메뉴 목록 저장
        List<String> matchedMenus = findResult.stream().map(WebElement::getText).toList();

        WebElement stroeNameElement = null;
        try {
          stroeNameElement = driver.findElement(By.className("GHAhO"));
        } catch (NoSuchElementException nse) {
          // 네이버 플레이스의 경우 음식점 이름 안보이므로 탭 이동후 다시 확인
          // 첫번째 탭은 다른 요소가 가리고 있어 두번째 탭 클릭
          driver.findElements(By.className("PlaceHeader__link__slT-J")).get(1).click();
          stroeNameElement = driver.findElement(By.className("GHAhO"));
        }
        String storeName = stroeNameElement.getText();
        // 공유하기 버튼을 찾기 위해 맨 위로 이동
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
        WebElement shareBtn = driver.findElement(By.id("_btp.share"));
        shareBtn.click();
        WebElement shareElement = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.className("_spi_input_copyurl"))
        );
        String storeURL = shareElement.getText();
        matchResult = new StoreInfo(storeName, matchedMenus, storeURL);
      }

      driver.switchTo().defaultContent(); // 다음 store 검색을 위해 현재 store Iframe에서 빠져나옴
    } catch (Exception e) {
      log.error("메뉴 확인 중 오류 발생 : {}", e.getMessage(), e);
    }

    // 메뉴 없는 경우
    return matchResult;
  }
}
