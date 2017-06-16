package maven.InstagramVideoPhotoDownloader;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

public class Instagram {

	public String str_ig_account = "", str_target_folder = "", str_download_list = "";
	public int scroll_num = 0;
	private WebDriver driver;
	private File fileName;
	private byte[] b = new byte[1024];
	private int len, currentBits = 0;
	private String src = "", name = "", extensionName = "", tmpFname = "";
	private FileOutputStream fileOutStream;
	private BufferedInputStream bufInStream;
	private Pattern p = Pattern.compile("http[s]?://[a-zA-Z0-9-._]+(/[a-zA-Z0-9-._]+)+");
	private Matcher m;
	
	public static void main(String[] args)
	{
		try
		{
			Instagram ig = new Instagram();
			ig.str_ig_account = "gal_gadot"; //Instagram 的使用者帳號
			ig.str_target_folder = "/Users/darrenyang/Documents/workspace/InstagramVideoPhotoDownloader/downloads/" + ig.str_ig_account + "/"; //放置擷取後檔案的放置處
			ig.str_download_list = "download_list.txt"; //擷取出來的連結，列在檔案內
			ig.scroll_num = 5; //捲軸下滾幾次
			ig.setInit(); //初始化設定
			ArrayList<String> list = ig.clickHyperlink(); //觀察圖片和影片連結，並加以擷取
			ig.download(list); //下載檔案
			ig = null;
		}
		catch(Exception e)
		{
			 e.getStackTrace(); 
		}
	}
	
	/** 設定初始值 */
	public void setInit()
	{
		try
		{
			//目前在 MacOS 設定 chromedriver，可以換成其它 os 的相關 driver，請參閱 https://chromedriver.storage.googleapis.com/index.html?path=2.30/
			System.setProperty("webdriver.chrome.driver", "/Users/darrenyang/Documents/workspace/InstagramVideoPhotoDownloader/plugin/chromedriver");
			
			//新增實體
			this.driver = new ChromeDriver();
			
			//導向 instagram 的網址
			this.driver.get("https://www.instagram.com/" + this.str_ig_account + "/");
			
			//設定使用者 folder，沒有時會自動增加
			this.fileName = new File(this.str_target_folder);
			if (!this.fileName.exists()) this.fileName.mkdirs();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			this.fileName = null;
		}
	}
	
	/** 執行擷取作業 */
	public String getHTML()
	{
		String contents = "";
		try
		{
			JavascriptExecutor jse = (JavascriptExecutor) this.driver;
			
			/*
			 * 1. By ID: in Java: driver.findElement(By.id("element id"))
			 * 
			 * 2. By CLASS: in Java: driver.findElement(By.className("element class"
			 * ))
			 * 
			 * 3. By NAME: in Java: driver.findElement(By.name("element name"))
			 * 
			 * 4. By TAGNAME: in Java: driver.findElement(By.tagName(
			 * "element html tag name"))
			 * 
			 * 5. By CSS Selector: in Java: driver.findElement(By.cssSelector(
			 * "css selector"))
			 * 
			 * 6. By Link: in Java: driver.findElement(By.link("link text"))
			 * 
			 * 7. By XPath: in Java: driver.findElement(By.xpath("xpath expression"
			 * ))
			 */
			
			WebElement elm_more_01 = this.driver.findElement(By.className("_oidfu")); //找尋連結:「更多」
			jse.executeScript("arguments[0].click();", elm_more_01);//執行點選「更多」

			try
			{
				for (int i = 1; i <= this.scroll_num; i++) 
				{
					jse.executeScript("window.scrollBy(0,1000)", "");
					Thread.sleep(3000);
				}
			}
			catch(Exception e){ e.getStackTrace(); }
			
			WebElement elm_body = this.driver.findElement(By.tagName("body"));
			contents = (String) jse.executeScript("return arguments[0].innerHTML;", elm_body);

			/*
			 * 下面是讓 Browser Automation 幫你在網頁上，針對特定元素(例如文字欄位或按鈕)，執行腳本設定的行為，但本例中沒用到，所以先行註解
			 * WebElement searchBox = driver.findElement(By.name("q"));
			 * searchBox.sendKeys("ChromeDriver");
			 * searchBox.submit();
			 * Thread.sleep(5000); // Let the user actually see something!
			*/
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			this.driver.close();
			this.driver.quit();
		}
		
		return contents;
	}
	
	/** 觀察圖片和影片連結，並加以擷取 */
	public ArrayList<String> clickHyperlink()
	{
		ArrayList<String> list = new ArrayList<String>();
		try
		{
			JavascriptExecutor js = (JavascriptExecutor) this.driver;
			
			/*
			 * 1. By ID: in Java: driver.findElement(By.id("element id"))
			 * 2. By CLASS: in Java: driver.findElement(By.className("element class"))
			 * 3. By NAME: in Java: driver.findElement(By.name("element name"))
			 * 4. By TAGNAME: in Java: driver.findElement(By.tagName("element html tag name"))
			 * 5. By CSS Selector: in Java: driver.findElement(By.cssSelector("css selector"))
			 * 6. By Link: in Java: driver.findElement(By.link("link text"))
			 * 7. By XPath: in Java: driver.findElement(By.xpath("xpath expression"))
			 */
			
			WebElement elm_more = this.driver.findElement(By.className("_8imhp")); //找尋連結:「更多」
			js.executeScript("arguments[0].click();", elm_more);//執行點選「更多」
			
			try
			{
				//下捲數次，確定 IG 可點選圖示數量
				for (int i = 1; i <= this.scroll_num; i++) 
				{
					js.executeScript("window.scrollBy(0,1000)", "");
					Thread.sleep(2000);
				}
			}
			catch(Exception e)
			{
				e.getStackTrace(); 
			}
			
			
			//檢查有幾個照片連結
			List<WebElement> elm_a = (List<WebElement>) this.driver.findElements(By.cssSelector("div._myci9 a._8mlbc"));
			
			if( elm_a.size() > 0 )
			{
				for(int i = 0; i < elm_a.size(); i++)
				{
					js.executeScript("arguments[0][" + i + "].click();", elm_a); //點選照片連結
					Thread.sleep(3000);
		
					List<WebElement> elm_check_video = (List<WebElement>) this.driver.findElements(By.cssSelector("video")); //看看是否有影片
					
					//若有影片，通常附近旁也有照片連結
					if( elm_check_video.size() > 0 )
					{
						//擷取影片連結
						List<WebElement> elm_video = (List<WebElement>) this.driver.findElements(By.cssSelector("div._2tomm > video")); 
						if(elm_video.size() > 0)
						{
							String str_video_link = (String) elm_video.get(0).getAttribute("src");
							list.add(str_video_link);
							System.out.println(str_video_link);
						}
						
						//擷取照片連結
						List<WebElement> elm_pic = (List<WebElement>) this.driver.findElements(By.cssSelector("div._2tomm > img")); 
						if(elm_pic.size() > 0)
						{
							String str_pic_link = (String) elm_pic.get(0).getAttribute("src");
							list.add(str_pic_link);
							System.out.println(str_pic_link);
						}
					}
					else
					{
						//擷取照片連結
						try
						{
							List<WebElement> elm_pic = (List<WebElement>) this.driver.findElements(By.cssSelector("article._j5hrx div._jjzlb > img._icyx7"));
							if(elm_pic.size() > 0)
							{
								String str_pic_link = (String) elm_pic.get(0).getAttribute("src");
								list.add(str_pic_link);
								System.out.println(str_pic_link);
							}
						}
						catch(Exception e)
						{
							e.printStackTrace();
						}
						
					}
					
					List<WebElement> elm_btn_close = (List<WebElement>) this.driver.findElements(By.cssSelector("button._3eajp")); //點擊關閉按鈕
					if(elm_btn_close.size() > 0) js.executeScript("arguments[0][0].click();", elm_btn_close); //執行關閉
					Thread.sleep(1000);
				}
			}
			
			
			this.driver.close();
			this.driver = null;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return list;
	}
	
	/** 下載連結檔案（含影片、照片） */
	public void download(ArrayList<String> list)
	{
		try
		{
			// Create a new trust manager that trust all certificates
			TrustManager[] trustAllCerts = new TrustManager[]{
			    new X509TrustManager() {
			        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			            return null;
			        }
			        public void checkClientTrusted(
			            java.security.cert.X509Certificate[] certs, String authType) {
			        }
			        public void checkServerTrusted(
			            java.security.cert.X509Certificate[] certs, String authType) {
			        }
			    }
			};

			// Activate the new trust manager
			try {
			    SSLContext sc = SSLContext.getInstance("SSL");
			    sc.init(null, trustAllCerts, new java.security.SecureRandom());
			    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			} catch (Exception e) {}
			
			PrintWriter file = new PrintWriter(new BufferedWriter(new FileWriter(this.str_target_folder + this.str_download_list, true)));
			
			this.b = new byte[1024];
			this.len = this.currentBits = 0;
			this.src = this.name = this.extensionName = this.tmpFname = "";
			
			if (list.size() > 0) 
			{				
				for (int i = 0; i < list.size(); i++) 
				{
					this.src = list.get(i);
					this.m = this.p.matcher(src);
					this.name = this.extensionName = this.tmpFname = "";
					
					if (this.m.find()) 
					{
						// 準備寫入檔案
						try 
						{
							this.tmpFname = this.m.group(0);
							URL url = new URL(this.tmpFname);
							URLConnection connection = url.openConnection();
							this.bufInStream = new BufferedInputStream( connection.getInputStream() );
							
							this.extensionName = this.tmpFname.substring(this.tmpFname.lastIndexOf("."), this.tmpFname.length());// 取得副檔名
							this.name = getCurrentDateTime() + this.extensionName;
							
							String savePath = this.str_target_folder + "\\" + this.name;
							this.fileOutStream = new FileOutputStream(savePath);
							
							System.out.println("開始下載檔案[ " + m.group(0)+ " ]");
							while ( (this.len = this.bufInStream.read(this.b, 0, this.b.length)) != -1 ) 
							{						
								this.fileOutStream.write(this.b, 0, this.len);
								this.currentBits += this.len;
							
								System.out.println("目前下載量[ " + (this.currentBits / 1024) + " ]KB");
							}
							
							System.out.println("＊＊＊ 完整檔案大小[ " + (this.currentBits / 1024) + " ]KB ＊＊＊");
							
							//寫入連結至 download_list
							file.println(this.m.group(0));
							
							if (this.fileOutStream != null) this.fileOutStream.close();
							if (this.bufInStream != null) this.bufInStream.close();
							
							this.fileOutStream = null;
							this.bufInStream = null;
							
							//暫時休息一下，再繼續跑迴圈
							Thread.sleep(1500);
							
							this.currentBits = 0;
							
						} catch (Exception e) {e.printStackTrace();}
					}
					this.src = "";
				}
			}
			file.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	// 取得現在時間
	public static String getCurrentDateTime() 
	{
		String str_time = "";
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			Date date = new Date();
			// System.out.println(dateFormat.format(date)); //2014/08/06
			// 15:59:48
			str_time = dateFormat.format(date).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return str_time;
	}
}