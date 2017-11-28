package controllers

import java.time.ZonedDateTime
import javax.inject.Inject

import de.proteinevolution.models.database.CMS.FeaturedArticle
import de.proteinevolution.models.database.results.{ Hmmer, PSIBlast }
import de.proteinevolution.db.{ MongoStore, ResultFileAccessor }
import de.proteinevolution.models.ToolNames
import play.api.libs.json.Json
import play.api.mvc._
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext

class DataController @Inject()(mongoStore: MongoStore,
                               psiblastController: PSIBlastController,
                               hmmerController: HmmerController,
                               hmmer: Hmmer,
                               psi: PSIBlast,
                               cc: ControllerComponents,
                               resultFiles: ResultFileAccessor)(implicit ec: ExecutionContext)
    extends AbstractController(cc) {

  /** Check whether the user is allowed to fetch the data for the particular job and retrieves the data with
   * stored given a particular key
   */
  def get(jobID: String): Action[AnyContent] = Action.async {
    resultFiles.getResults(jobID).map {
      case Some(jsValue) => Ok(jsValue)
      case None          => NotFound
    }
  }

  /**
   * Action to fetch article with articleID from database
   */
  def fetchArticle(articleID: String): Action[AnyContent] = Action.async {
    mongoStore.getArticle(articleID).map {
      case Some(realArticle) => NoContent
      case None              => NotFound
    }
  }

  /**
   * Action to fetch the last N recent articles database
   */
  def getRecentArticles(numArticles: Int): Action[AnyContent] = Action.async {
    mongoStore.getArticles(numArticles).map { seq =>
      Ok(Json.toJson(seq))
    }
  }

  /**
   * Action to write an article into the database
   */
  def writeArticle(title: String, text: String, textlong: String, link: String, imagePath: String): Action[AnyContent] =
    Action.async {
      // TODO ensure that only authorized people can write a front page article
      val article =
        FeaturedArticle(BSONObjectID.generate(), title, text, textlong, link, imagePath, Some(ZonedDateTime.now), None)
      mongoStore.writeArticleDatabase(article).map { wr =>
        if (wr.ok) {
          NoContent
        } else {
          BadRequest
        }
      }
    }

  def getHelp(toolname: String) = Action {
    val help = toolname match {
      case ToolNames.PSIBLAST.value            => views.html.help.psiblast()
      case ToolNames.HHBLITS.value             => views.html.help.hhblits()
      case ToolNames.HHPRED.value              => views.html.help.hhpred()
      case ToolNames.HMMER.value               => views.html.help.hmmer()
      case ToolNames.PATSEARCH.value           => views.html.help.patsearch()
      case ToolNames.CLUSTALO.value            => views.html.help.clustalo()
      case ToolNames.KALIGN.value              => views.html.help.kalign()
      case ToolNames.MAFFT.value               => views.html.help.mafft()
      case ToolNames.MSAPROBS.value            => views.html.help.msaprobs()
      case ToolNames.MUSCLE.value              => views.html.help.muscle()
      case ToolNames.TCOFFEE.value             => views.html.help.tcoffee()
      case ToolNames.ALN2PLOT.value            => views.html.help.aln2plot()
      case ToolNames.HHREPID.value             => views.html.help.hhrepid()
      case ToolNames.MARCOIL.value             => views.html.help.marcoil()
      case ToolNames.PCOILS.value              => views.html.help.pcoils()
      case ToolNames.REPPER.value              => views.html.help.repper()
      case ToolNames.TPRPRED.value             => views.html.help.tprpred()
      case ToolNames.ALI2D.value               => views.html.help.ali2d()
      case ToolNames.QUICK2D.value             => views.html.help.quick2d()
      case ToolNames.MODELLER.value            => views.html.help.modeller()
      case ToolNames.SAMCC.value               => views.html.help.samcc()
      case ToolNames.ANCESCON.value            => views.html.help.ancescon()
      case ToolNames.CLANS.value               => views.html.help.clans()
      case ToolNames.MMSEQS2.value             => views.html.help.mmseqs2()
      case ToolNames.PHYML.value               => views.html.help.phyml()
      case ToolNames.SIXFRAMETRANSLATION.value => views.html.help.sixframe()
      case ToolNames.BACKTRANS.value           => views.html.help.backtrans()
      case ToolNames.HHFILTER.value            => views.html.help.hhfilter()
      case ToolNames.RETSEQ.value              => views.html.help.retseq()
      case ToolNames.SEQ2ID.value              => views.html.help.seq2id()
    }
    Ok(help)
  }

}
