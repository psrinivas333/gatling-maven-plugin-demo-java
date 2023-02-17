package gatlingdemostoreapi;

import java.time.Duration;
import java.util.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import io.gatling.javaapi.jdbc.*;
import scala.Product;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
import static io.gatling.javaapi.jdbc.JdbcDsl.*;

public class DemoStoreApiSimulation extends Simulation {

  private HttpProtocolBuilder httpProtocol = http
    .baseUrl("http://demostore.gatling.io")
    .inferHtmlResources()
    .acceptHeader("*/*")
    //.acceptHeader("Cache-Control,no-cache")
    .acceptHeader("application/json")
    //.acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("PostmanRuntime/7.30.1");

  private static Map<CharSequence, String> headers_2 = Map.ofEntries(
    Map.entry("Cache-Control", "no-cache"),
    Map.entry("Content-Type", "application/json")
  //  Map.entry("Postman-Token", "13319cbd-747e-4e96-b087-8d9a2437de48")
  );
  
  private static Map<CharSequence, String> Authenticate = Map.ofEntries(
    Map.entry("Cache-Control", "no-cache"),
    Map.entry("Content-Type", "application/json"),
   // Map.entry("Postman-Token", "c34d365a-a803-464f-8e2c-009e1bcc87c8"),
    Map.entry("authorization", "Bearer #{jwt}")
  );

  private static ChainBuilder initSession = exec(session->session.set("authenticated", false));

  private static class Authentication {
    private static ChainBuilder authenticate = 
    doIf(session->!session.getBoolean("authenticated")).then(
    exec(http("Authenticate")
        .post("/api/authenticate")
       // .header("Content-Type", "application/json")
        .headers(headers_2)
        .body(RawFileBody("gatlingdemostoreapi/demostoreapisimulation/0002_request.json"))
        .check(status().is(200))
        .check(jsonPath("$.token").saveAs("jwt")))
        .exec(session->session.set("authenticated", true)));
  }
//Feeder Code
  
  private static class Category {
    private static FeederBuilder.Batchable<String> categoriesfeeder = 
        csv("Data/categories.csv").random();
    private static ChainBuilder CategoryList =
    exec(http("CategoryList")
        .get("/api/category")
        .check(jsonPath("$[?(@.id==5)].name").is("For Him"))
        .check(jsonPath("$[?(@.id==6)].name").is("For Her"))
        .check(jsonPath("$[?(@.id==7)].name").is("Unisex")));
    
    private static ChainBuilder GetCategory=
    exec(http("GetCategory")
        .get("/api/category/6")
        .check(jsonPath("$.name").is("For Her")));
    
        private static ChainBuilder CreateCategory=
    exec(Authentication.authenticate)
    .exec(http("CreateCategory")
        .post("/api/category")
        .headers(Authenticate)
        .body(RawFileBody("gatlingdemostoreapi/demostoreapisimulation/0003_request.json"))
        .check(jsonPath("$.name").is("Alien")));
   
        private static ChainBuilder UpdateCategory=
        feed(categoriesfeeder)
        .exec(Authentication.authenticate)
        .exec(http("UpdateCategory")
        .put("/api/category/#{categoryId}")
        .headers(Authenticate)
        .body(RawFileBody("gatlingdemostoreapi/demostoreapisimulation/0004_request.json"))
        .check(jsonPath("$.name").is("#{categoryName}")));
  }

  private static class Products{
    private static FeederBuilder.Batchable<String> productsFeeder =
    csv("Data/products.csv").circular();
  
    private static ChainBuilder ProductList=
    exec(http("ProductList")
        .get("/api/product")
     //   .check(jsonPath("$[?(@.id==17)].name)".is("Casual Black-Blue")));
        .check(jsonPath("$[?(@.id==17)].name").is("Casual Black-Blue"))
        .check(jmesPath("[*].id").ofList().saveAs("allProductsIds")));
    
        private static ChainBuilder ListPorductsByCategory=
         exec(http("ListPorductsByCategory")
        .get("/api/product?category=7")
        
        .check(jsonPath("$[?(@.id==33)].name").is("Curved Brown")));
    
        private static ChainBuilder GetProduct=
        exec(session -> {
          List<Integer> allProductsIds = session.getList("allProductsIds");
         return session.set("productID",allProductsIds.get(new Random().nextInt(allProductsIds.size())));
          })
          .exec(
            session -> {
            //  System.out.println("allProductsIds captured:"+ session.get("allProductsIds").toString());
              System.out.println("allProductsIds captured:"+ session.get("allProductsIds").toString());
              System.out.println("allProductsIds captured:" + session.get("productID").toString());
              return session;
            }
          )
       .exec(
         http("GetProduct")
        .get("/api/product/#{productID}")
        .check(jsonPath("$.id").isEL("#{productID}"))
        .check(jmesPath("@").ofMap().saveAs("product")))
                .exec(
               session ->{
                   System.out.println("value of product:" + session.get("product").toString());
                   return session;
               }
       );

    private static ChainBuilder Updateproduct=
    exec(Authentication.authenticate)
            .exec(
                    session ->{
                        Map<String, Object> product = session.getMap("product");
                        return session
                        .set("productCategoryId",product.get("categoryId"))
                        .set("productName",product.get("name"))
                        .set("productDescription",product.get("description"))
                        .set("productImage",product.get("image"))
                        .set("productPrice",product.get("price"))
                        .set("productId",product.get("id"));
      })
        .exec(http("Updateproduct #{productName}")
        .put("/api/product/#{productId}")
      //  .headers(headers_9)
        .headers(Authenticate)
        .body(RawFileBody("gatlingdemostoreapi/demostoreapisimulation/0009_request.json"))
        .check(jsonPath("$.name").is("My new product")));

        private static ChainBuilder Createproduct=
        exec(Authentication.authenticate)
        .feed(productsFeeder)
        .exec(http("CreateProduct #{productName}")
        .post("/api/product")
     //   .headers(headers_8)
        .headers(Authenticate)
        .body(ElFileBody("gatlingdemostoreapi/demostoreapisimulation/CreateProduct.json")));
    //    .check(jsonPath("$.name").is("#{productName}")));
  }


  private ScenarioBuilder scn = scenario("DemoStoreApiSimulation")
    .exec(initSession)
    .pause(5)
    .exec(Category.CategoryList)
    .pause(15)
    .exec(Category.GetCategory)
    .pause(18)
    .exec(Authentication.authenticate)
    .pause(11)
    .exec(Category.CreateCategory)
    .pause(13)
    .exec(Category.UpdateCategory)
    .pause(11)
    .exec(Products.ProductList)
    .pause(12)
    .exec(Products.ListPorductsByCategory)
    .pause(11)
    .exec(Products.GetProduct)
    .pause(9)
    .repeat(4, "ProductCount").on(exec(Products.Createproduct))
    .pause(10)
    .exec(Products.Updateproduct );
  {
	  setUp(scn.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
  }
}
