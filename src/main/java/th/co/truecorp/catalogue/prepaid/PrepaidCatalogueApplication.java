/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package th.co.truecorp.catalogue.prepaid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

@SpringBootApplication
@EnableScheduling
public class PrepaidCatalogueApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrepaidCatalogueApplication.class, args);
        
    }
        
    /*@Bean
    public WebClient createWebClient() throws SSLException {
        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
        
//        ConnectionProvider connectionProvider = ConnectionProvider.builder("connectionProvider")
//                .maxIdleTime(Duration.ofSeconds(10))
//                .build();
//        HttpClient httpClient = HttpClient.create(connectionProvider).secure(t -> t.sslContext(sslContext));
//        return WebClient.builder()
//                .clientConnector(new ReactorClientHttpConnector(httpClient))
//                .build();

        HttpClient httpConnector = HttpClient.create().secure(t -> t.sslContext(sslContext));
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpConnector))
                .build();
    }*/
    
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .clientConnector(conn())
                .build();
    }
    
    private ClientHttpConnector conn() {
        return new ReactorClientHttpConnector(HttpClient.from(TcpClient.newConnection()));
    }
}
