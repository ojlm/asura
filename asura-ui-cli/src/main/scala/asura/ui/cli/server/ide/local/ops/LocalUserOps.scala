package asura.ui.cli.server.ide.local.ops

import scala.concurrent.{ExecutionContext, Future}

import asura.ui.cli.server.ide.local.{LocalIde, LocalStore}
import asura.ui.cli.store.lucene.field.FieldType
import asura.ui.cli.store.lucene.query.SearchResult
import asura.ui.cli.store.lucene.{DocumentBuilder, term}
import asura.ui.ide.model.UserPreference.LatestPreference
import asura.ui.ide.model.{Activity, UserPreference, Workspace}
import asura.ui.ide.ops.ActivityOps.QueryActivity
import asura.ui.ide.ops.UserOps

class LocalUserOps(val ide: LocalIde)(implicit ec: ExecutionContext)
  extends LocalStore[UserPreference](ide.config.PATH_USER) with UserOps {

  val username = define.field[String]("username", FieldType.UN_TOKENIZED)
  val alias = define.field[String]("alias")
  val description = define.field[String]("description", fullTextSearchable = true, sortable = false)
  val email = define.field[String]("email", FieldType.UN_TOKENIZED, sortable = false)
  val avatar = define.field[String]("avatar", FieldType.UN_TOKENIZED, sortable = false)

  val docToModel: SearchResult => UserPreference = doc => {
    val item = UserPreference(
      username = doc(username),
      alias = doc(alias),
      description = doc(description),
      email = doc(email),
      avatar = doc(avatar),
    )
    fillCommonField(item, doc)
  }

  val modelToDoc: UserPreference => DocumentBuilder = item => {
    val builder = doc().fields(
      username(item.username),
      alias(item.alias),
      description(item.description),
      email(item.email),
      avatar(item.avatar),
    )
    fillCommonField(builder, item)
  }

  override def getPreference(username: String): Future[UserPreference] = {
    Future.successful {
      val results = query(docToModel).filter(term(this.username(username))).limit(1).search()
      val preference = if (results.total > 0) {
        val user = results.entries.head
        val activity = ide.activity.searchSync(QueryActivity(creator = username)).list.head
        user.latest = LatestPreference(activity.workspace)
        user
      } else { // init
        val userItem = UserPreference(username = username)
        insert(userItem)
        val workspaceItem = Workspace(name = username)
        ide.workspace.insert(workspaceItem)
        ide.activity.insert(Activity(workspace = username, op = Activity.OP_INSERT_WORKSPACE))
        UserPreference(username, latest = LatestPreference(username))
      }
      preference
    }
  }

  override def insert(item: UserPreference): Future[Long] = {
    Future {
      index(modelToDoc(item))
    }
  }

}
