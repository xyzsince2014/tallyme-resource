package tokyomap.resource.controller

import com.tokyomap.resource.application.ResourceService
import com.tokyomap.resource.controller.resourceRoutes
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals

class ResourceControllerTest {

  private val resourceServiceMock = mockk<ResourceService>()

  @Test
  fun `GET userinfo with valid Bearer token returns 200 OK`() = testApplication {
    // FORCE the receiver by using this@application
    application {
      this@application.install(Koin) {
        modules(module {
          single { resourceServiceMock }
        })
      }
      this@application.routing {
        resourceRoutes()
      }
    }

    val mockJson = buildJsonObject {
      put("sub", "12345")
    }

    coEvery { resourceServiceMock.getUserInfo(any()) } returns mockJson

    val response = client.get("/userinfo") {
      header(HttpHeaders.Authorization, "Bearer valid-token")
    }

    assertEquals(HttpStatusCode.OK, response.status)
  }

  @Test
  fun `GET userinfo with missing token returns 401 Unauthorized`() = testApplication {
    application {
      this@application.install(Koin) {
        modules(module { single { resourceServiceMock } })
      }
      this@application.routing {
        resourceRoutes()
      }
    }

    val response = client.get("/userinfo")
    assertEquals(HttpStatusCode.Unauthorized, response.status)
  }
}