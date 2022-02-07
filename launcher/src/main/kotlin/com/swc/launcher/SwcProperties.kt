import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "kubernetes")
data class SwcProperties(
    val zookeeper: Service,
    val kafka: Service,
    val mongo: Service,
    val `transfer-app`: Service,
    val `kotlin-rest`: Service,
    val `react-client`: Service
){
    data class Service(
        var host: String,
        var port: Int,
    )
}


