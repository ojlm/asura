package asura.app.api.model

case class UserProfile(
                        var token: String,
                        var username: String,
                        var nickname: String = null,
                        var email: String = null,
                        var summary: String = null,
                        var description: String = null,
                        var avatar: String = null,
                      )
