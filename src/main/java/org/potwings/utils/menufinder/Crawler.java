package org.potwings.utils.menufinder;

import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Crawler {

  private static final String findStore = "강남역 호프";
  private static final String findMenu = "해물고기한판";

  public static void main(String[] args) {
    System.setProperty("webdriver.chrome.driver", "C:/zzz/chromedriver-win64/chromedriver.exe");

    ChromeOptions options = new ChromeOptions();
    options.addArguments("--start-maximized");
    options.addArguments("--remote-allow-origins=*");
    WebDriver driver = new ChromeDriver(options);
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    try {
      driver.get("https://map.naver.com");  // 실제 URL로 바꿔주세요

      // 1. 검색 수행
      WebElement inputSearch = wait.until(
          ExpectedConditions.presenceOfElementLocated(By.className("input_search")));
      inputSearch.clear();
      inputSearch.sendKeys(findStore);
      inputSearch.sendKeys(Keys.ENTER);

      WebElement searchIframe = wait.until(
          ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#searchIframe")));
      driver.switchTo().frame(searchIframe); // 검색 결과 iframe으로 이동

      // 스크롤 실행
      JavascriptExecutor js = (JavascriptExecutor) driver;
      // 무한스크롤이 적용된 div 선택자 (직접 확인 필요)
      WebElement scrollDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(
          By.cssSelector("#_pcmap_list_scroll_container"))); // 해당 페이지의 모든 요소들을 불러오기 위해 스크롤

      long scrollTop = 0;
      while (true) {
        // 컨텐츠 로딩 속도를 위해 스크롤 천천히 아래로 이동
        js.executeScript("arguments[0].scrollTop += arguments[1];", scrollDiv, 700);

        // 약간의 대기 시간 (렌더링 시간 고려)
        Thread.sleep(1000);

        long newScrollTop = (long) js.executeScript("return arguments[0].scrollTop", scrollDiv);
        if (scrollTop == newScrollTop) {
          break;
        }
        scrollTop = newScrollTop;
      }
      // 음식점 목록 추출
      List<WebElement> storeList = driver.findElements(By.className("iHXbN"));
      for (WebElement store : storeList) {
        wait.until(ExpectedConditions.elementToBeClickable(store)).click();
        Thread.sleep(3000); // 클릭 후 로딩 대기
        processMenuAndMore(driver, wait);  // 메뉴 클릭 및 GHAhO 값 수집
        driver.switchTo().frame(searchIframe); // 검색 iframe으로 이동
      }
//      while (true) {
//        boolean ouxiqProcessed = false;
//
//        // 2. 현재 페이지의 모든 ouxiq 요소 순회
//        List<WebElement> ouxiqList = driver.findElements(By.className("iHXbN"));
//        for (WebElement ouxiq : ouxiqList) {
//          try {
//            wait.until(ExpectedConditions.elementToBeClickable(ouxiq)).click();
//            Thread.sleep(500); // 클릭 후 로딩 대기
//            driver.switchTo().defaultContent();
//            processMenuAndMore(driver, wait);  // 메뉴 클릭 및 GHAhO 값 수집
//            driver.switchTo().frame(searchIframe);
//            driver.navigate().back();  // 뒤로 가서 다음 ouxiq 처리
//            wait.until(ExpectedConditions.presenceOfElementLocated(By.className("ouxiq")));
//            ouxiqProcessed = true;
//          } catch (Exception e) {
//            // 클릭 불가능한 경우 무시
//          }
//        }
//
//        // 3. ouxiq 요소를 모두 순회한 경우 다음 페이지 시도
//        if (!ouxiqProcessed) {
//          try {
//            WebElement nextPage = wait.until(
//                ExpectedConditions.elementToBeClickable(By.className("eUTV2")));
//            nextPage.click();
//            wait.until(ExpectedConditions.presenceOfElementLocated(By.className("ouxiq")));
//            Thread.sleep(1000); // 로딩 대기
//          } catch (Exception e) {
//            System.out.println("더 이상 eUTV2 페이지가 없습니다. 종료합니다.");
//            break;
//          }
//        }
//      }

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      driver.quit();
    }
  }

  // "메뉴" 클릭 및 "더보기" 반복 클릭, GHAhO 수집
  private static void processMenuAndMore(WebDriver driver, WebDriverWait wait) {
    try {
      driver.switchTo().defaultContent(); //storeIframe을 찾을 수 있도록 전체 화면으로 driver 이동
      WebElement storeIframe = wait.until(
          ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#entryIframe")));
      driver.switchTo().frame(storeIframe); // 음식점 iframe으로 이동
      List<WebElement> spans = driver.findElements(By.className("veBoZ"));
      for (WebElement span : spans) {
        if (span.getText().equals("메뉴")) {
          System.out.println(span);
          span.click();
          break;
        }
      }

      WebElement moreMenu = null;
      while (true) {
        try {
          moreMenu = driver.findElement(By.className("TeItc"));
        } catch (NoSuchElementException e) {
          System.out.println("더보기 완료");
          break;
        }
        moreMenu.click();
        Thread.sleep(3000L); // 로딩 대기
      }

      // 메뉴 목록 추출
      List<WebElement> menuList = driver.findElements(By.className("lPzHi"));

      boolean haveMenu = false;
      StringBuffer sb = null;
      for (WebElement menu : menuList) {
        String menuStr = menu.getText();
        if (menuStr.contains(findMenu)) {
          if (!haveMenu) {
            haveMenu = true;
            sb = new StringBuffer();
          }
          sb.append(menuStr).append(", ");
        }
      }

      if (sb != null) {
        sb.append("음식점 명 : ").append(driver.findElement(By.className("GHAhO")).getText());
        System.out.println(sb.toString());
      }

      driver.switchTo().defaultContent(); // 다음 store 검색을 위해 현재 store Iframe에서 빠져나옴
    } catch (Exception e) {
      System.out.println("메뉴 클릭 처리 중 오류 발생");
      e.printStackTrace();
    }
  }
}
