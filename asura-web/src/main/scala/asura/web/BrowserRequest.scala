package asura.web

case class BrowserRequest(id: String, url: String)

case class BrowserRequests(requests: Seq[BrowserRequest])
