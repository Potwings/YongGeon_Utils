package org.potwings.utils.menufinder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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

  private static final String findStore = "가산역 호프";
  private static List<String> findMenuList = new ArrayList<>();

  private static List<String> resultList = new ArrayList<>();

  public static void main(String[] args) {

    System.setProperty("webdriver.chrome.driver", "C:/zzz/chromedriver-win64/chromedriver.exe");

    String findMenu = "아이스크림, 빙수";

    if (findMenu.contains(",")) {
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

      while (true) {
        // 모든 음식점 목록을 불러오기 위해 스크롤 진행
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebElement scrollDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("#_pcmap_list_scroll_container")));

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

        // 음식점 목록 로딩 완료 후 결과 추출
        List<WebElement> storeList = driver.findElements(By.className("iHXbN"));
        for (WebElement store : storeList) {
          wait.until(ExpectedConditions.elementToBeClickable(store)).click();
          Thread.sleep(3000); // 클릭 후 로딩 대기
          processMenuAndMore(driver, wait);  // 메뉴 클릭 및 GHAhO 값 수집
          driver.switchTo().frame(searchIframe); // 검색 iframe으로 이동
        }

        WebElement nextPageBtn = driver.findElements(By.className("eUTV2")).get(1);
        boolean hasNextPage = nextPageBtn.getAttribute("aria-disabled").equals("false");
        if (!hasNextPage) {
          // 더 이상 확인할 다음 페이지가 없는 경우 중단
          break;
        }
        nextPageBtn.click(); // 다음 페이지로 이동
      }
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
          break;
        }
        moreMenu.click();
        Thread.sleep(3000L); // 로딩 대기
      }

      // 메뉴 목록 추출
      List<WebElement> menuList = driver.findElements(By.className("lPzHi"));

      List<WebElement> findResult = menuList.stream()
          .filter(el -> {
            String text = el.getText();
            return findMenuList.stream().anyMatch(text::contains);
          })
          .collect(Collectors.toList());

      if (!findResult.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        sb.append("음식점 명 : ").append(driver.findElement(By.className("GHAhO")).getText());
        findResult.forEach(result -> sb.append(result).append(", "));
        resultList.add(sb.toString());
      }

      driver.switchTo().defaultContent(); // 다음 store 검색을 위해 현재 store Iframe에서 빠져나옴
    } catch (Exception e) {
      System.out.println("메뉴 클릭 처리 중 오류 발생");
      e.printStackTrace();
    }

    for (String result : resultList) {
      System.out.println(result);
    }
  }
}
