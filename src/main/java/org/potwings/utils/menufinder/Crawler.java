package org.potwings.utils.menufinder;

import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
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
  private static final String findMenu = "치킨";

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
      driver.switchTo().frame(searchIframe);
      List<WebElement> ouxiqList = driver.findElements(By.className("iHXbN"));
      wait.until(ExpectedConditions.elementToBeClickable(ouxiqList.get(0))).click();
      Thread.sleep(500); // 클릭 후 로딩 대기
      driver.switchTo().defaultContent();
      processMenuAndMore(driver, wait);  // 메뉴 클릭 및 GHAhO 값 수집
      driver.switchTo().frame(searchIframe);
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
      WebElement searchIframe = wait.until(
          ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#entryIframe")));
      driver.switchTo().frame(searchIframe);
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
          if(!haveMenu) {
            haveMenu = true;
            sb = new StringBuffer();
          }
          System.out.println("================");
          sb.append(menuStr);
          sb.append(",");
          System.out.println("================");
        }
      }

    } catch (Exception e) {
      System.out.println("메뉴 클릭 처리 중 오류 발생");
      e.printStackTrace();
    }
  }
}
