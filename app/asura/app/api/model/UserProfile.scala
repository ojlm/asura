package asura.app.api.model

case class UserProfile(
                        val token: String,
                        val username: String,
                        val email: String,
                      ) {

}
