package models.results

import javax.inject.Inject
import better.files._
import models.Constants
import models.database.results.AlignmentResult
import play.twirl.api.Html
import play.api.Logger
import models.database.results._
import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

object BlastVisualization  {

  private val color_regex = """(?:[WYF]+|[LIVM]+|[AST]+|[KR]+|[DE]+|[QN]+|H+|C+|P+|G+)""".r
  private val CC_pattern = """(C+)""".r("C")
  private val TM_pattern = """(M+)""".r("M")
  private val DO_pattern = """(D+)""".r("D")
  private val helix_pattern = """([Hh]+)""".r
  private val sheet_pattern = """([Ee]+)""".r
  private val helix_sheets = """([Hh]+|[Ee]+)""".r("ss")

  private val uniprotReg = """([A-Z0-9]{10}|[A-Z0-9]{6})""".r
  private val scopReg = """([defgh][0-9a-zA-Z\.\_]+)""".r
  private val smartReg = """(^SM0[0-9]{4})""".r
  private val ncbiCDReg   = """(^[cs]d[0-9]{5})""".r
  private val cogkogReg   = """(^[CK]OG[0-9]{4})""".r
  private val tigrReg   = """(^TIGR[0-9]{5})""".r
  private val prkReg = """(CHL|MTH|PHA|PLN|PTZ|PRK)[0-9]{5}""".r
  private val mmcifReg = """(...._[0-9a-zA-Z][0-9a-zA-Z]?[0-9a-zA-Z]?[0-9a-zA-Z]?)""".r
  private val mmcifShortReg = """([0-9]+)""".r
  private val pfamReg = """(pfam[0-9]+|PF[0-9]+(\.[0-9]+)?)""".r
  private val ncbiReg = """[A-Z]{2}_?[0-9]+\.?\#?([0-9]+)?|[A-Z]{3}[0-9]{5}?\.[0-9]""".r
  
  private val envNrNameReg = """(env.*|nr.*)""".r
  private val pdbNameReg = """(pdb.*)""".r
  private val uniprotNameReg = """(uniprot.*)""".r
  private val pfamNameReg = """(Pfam.*)""".r

  private val pdbBaseLink = "http://pdb.rcsb.org/pdb/explore.do?structureId="
  private val pdbeBaseLink = "http://www.ebi.ac.uk/pdbe/entry/pdb/"
  private val ncbiBaseLink =
    "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?SUBMIT=y&db=structure&orig_db=structure&term="
  private val ncbiProteinBaseLink = "https://www.ncbi.nlm.nih.gov/protein/"
  private val scopBaseLink = "http://scop.berkeley.edu/sid="
  private val pfamBaseLink = "http://pfam.xfam.org/family/"
  private val cddBaseLink = "http://www.ncbi.nlm.nih.gov/Structure/cdd/cddsrv.cgi?uid="
  private val uniprotBaseLik = "http://www.uniprot.org/uniprot/"
  private val smartBaseLink = "http://smart.embl-heidelberg.de/smart/do_annotation.pl?DOMAIN="

  def SSColorReplace(sequence: String): String =
    this.helix_sheets.replaceAllIn(
      sequence, { m =>
        m.group("ss") match {
          case this.helix_pattern(substr) => "<span class=\"ss_e\">" + substr + "</span>"
          case this.sheet_pattern(substr) => "<span class=\"ss_h\">" + substr + "</span>"
        }
      }
    )

  def Q2DColorReplace(name: String, sequence: String): String =
         name match {
           case "psipred" => this.helix_sheets.replaceAllIn(
             sequence, { m =>
               m.group("ss") match {
                 case this.helix_pattern(substr) => "<span class=\"ss_e_b\">" + substr + "</span>"
                 case this.sheet_pattern(substr) => "<span class=\"ss_h_b\">" + substr + "</span>"
               }
             }
           )
           case "spider2" => this.helix_sheets.replaceAllIn(
             sequence, { m =>
               m.group("ss") match {
                 case this.helix_pattern(substr) => "<span class=\"ss_e_b\">" + substr + "</span>"
                 case this.sheet_pattern(substr) => "<span class=\"ss_h_b\">" + substr + "</span>"
               }
             }
           )

           case "marcoil" => this.CC_pattern.replaceAllIn(sequence, "<span class=\"CC_b\">" + "$1" + "</span>")
           case "coils" => this.CC_pattern.replaceAllIn(sequence, "<span class=\"CC_b\">" + "$1" + "</span>")
           case "pcoils" => this.CC_pattern.replaceAllIn(sequence, "<span class=\"CC_b\">" + "$1" + "</span>")
           case "tmhmm" => this.TM_pattern.replaceAllIn(sequence, "<span class=\"CC_m\">" + "$1" + "</span>")
           case "phobius" => this.TM_pattern.replaceAllIn(sequence, "<span class=\"CC_m\">" + "$1" + "</span>")
           case "polyphobius" => this.TM_pattern.replaceAllIn(sequence, "<span class=\"CC_m\">" + "$1" + "</span>")
           case "spotd" => this.DO_pattern.replaceAllIn(sequence, "<span class=\"CC_do\">" + "$1" + "</span>")
           case "iupred" => this.DO_pattern.replaceAllIn(sequence, "<span class=\"CC_do\">" + "$1" + "</span>")

         }



  def colorRegexReplacer(sequence: String): String =
    this.color_regex.replaceAllIn(sequence, { m =>
      "<span class=\"aa_" + m.toString().charAt(0) + "\">" + m.toString() + "</span>"
    })

  def makeRow(rowClass: String, entries: Array[String]): Html = {
    var html = ""
    if (rowClass == null)
      html += "<tr>"
    else
      html += "<tr class='" + rowClass + "'>"
    for (entry <- entries) {
      html += "<td>" + entry + "</td>"
    }
    html += "<tr>"
    Html(html)
  }

  /* GENERATING LINKS FOR HHPRED */

  def getSingleLink(id: String): Html = {
    val db = identifyDatabase(id)
    var link = ""
    val idTrimmed = if (id.length > 4) {
      id.slice(1, 5)
    } else {
      id
    }
    val idPfam = id.replaceAll("am.*$||..*", "")
    val idPdb = id.replaceAll("_.*$", "")
    if (db == "scop") {
      link += generateLink(scopBaseLink, id, id)
    } else if (db == "mmcif") {
      link += generateLink(pdbBaseLink, idPdb, id)
    } else if (db == "prk") {
      link += generateLink(cddBaseLink, id, id)
    } else if (db == "ncbicd") {
      link += generateLink(cddBaseLink, id, id)
    } else if (db == "cogkog") {
      link += generateLink(cddBaseLink, id, id)
    } else if (db == "tigr") {
      link += generateLink(cddBaseLink, id, id)
    } else if (db == "pfam") {
      link += generateLink(pfamBaseLink, idPfam + "#tabview=tab0", id)
    } else if (db == "ncbi") {
      link += generateLink(ncbiProteinBaseLink, id, id)
    } else if (db == "uniprot") {
      link += generateLink(uniprotBaseLik, id, id)
    } else if (db == "smart") {
      link += generateLink(smartBaseLink, id, id)
    } else {
      link = id
    }
    Html(link)
  }

  def getLinks(id: String): Html = {
    val db = identifyDatabase(id)
    var links = new ArrayBuffer[String]()
    var idNcbi = id.replaceAll("#", ".") + "?report=fasta"
    var idPdb = id.replaceAll("_.*$", "").toLowerCase
    val idTrimmed = if (id.length > 4) {
      id.slice(1, 5)
    } else {
      id
    }
    var idCDD = id.replaceAll("PF", "pfam")
    if (db == "scop") {
      links += generateLink(scopBaseLink, id, "SCOP")
      links += generateLink(ncbiBaseLink, idTrimmed, "NCBI")
    } else if (db == "mmcif") {
      links += generateLink(pdbeBaseLink, idPdb, "PDBe")
    } else if (db == "pfam") {
      idCDD = idCDD.replaceAll("\\..*", "")
      links += generateLink(cddBaseLink, idCDD, "CDD")
    } else if (db == "ncbi") {
      links += generateLink(ncbiProteinBaseLink, idNcbi, "NCBI Fasta")
    }

    Html(links.mkString(" | "))
  }

  def getSingleLinkDB(db: String, id: String): Html = {
    var link = ""
    val idTrimmed = if (id.length > 4) {
      id.slice(1, 5)
    } else {
      id
    }
    val idPfam = id.replaceAll("am.*$||..*", "")
    val idPdb = id.replaceAll("_.*$", "")
    db match {
      case envNrNameReg(_) => link += generateLink(ncbiProteinBaseLink, id, id)
      case pdbNameReg(_) => link += generateLink(pdbBaseLink, idPdb, id)
      case uniprotNameReg(_) => link += generateLink(uniprotBaseLik, id, id)
      case pfamNameReg(_) => link += generateLink(pfamBaseLink, idPfam + "#tabview=tab0", id)
      case _ => link = id
    }
    Html(link)
  }

  def getLinksDB(db: String, id: String): Html = {
    var links = new ArrayBuffer[String]()
    var idNcbi = id.replaceAll("#", ".") + "?report=fasta"
    var idPdb = id.replaceAll("_.*$", "").toLowerCase
    val idTrimmed = if (id.length > 4) {
      id.slice(1, 5)
    } else {
      id
    }
    var idCDD = id.replaceAll("PF", "pfam")

    db match {
      case envNrNameReg(_) => links += generateLink(ncbiProteinBaseLink, idNcbi, "NCBI Fasta")
      case pdbNameReg(_) => links += generateLink(pdbeBaseLink, idPdb, "PDBe")
      case pfamNameReg(_) => {
        idCDD = idCDD.replaceAll("\\..*", "")
        links += generateLink(cddBaseLink, idCDD, "CDD")
      }
      case uniprotNameReg(_) => ""
    }
    Html(links.mkString(" | "))
  }

  def getSingleLinkHHBlits(id: String): Html = {
    var link = ""
    val idPdb = id.replaceAll("_.*$", "")
    link += generateLink(uniprotBaseLik, id, id)
    Html(link)
  }

  def getLinksHHBlits(id: String): Html = {
    Html(
      "<a data-open=\"templateAlignmentModal\" onclick=\"templateAlignment(\'" + id + "\')\">Template alignment</a>"
    )
  }

  def getLinksHHpred(id: String): Html = {
    val db = identifyDatabase(id)
    var links = new ArrayBuffer[String]()

    var idPdb = id.replaceAll("_.*$", "").toLowerCase
    val idTrimmed = if (id.length > 4) {
      id.slice(1, 5)
    } else {
      id
    }
    var idCDD = id.replaceAll("PF", "pfam")
    var idNcbi = id.replaceAll("#", ".") + "?report=fasta"
    links += "<a data-open=\"templateAlignmentModal\" onclick=\"templateAlignment(\'" + id + "\')\">Template alignment</a>"
    if (db == "scop") {
      links += "<a data-open=\"structureModal\" onclick=\"showStructure(\'" + id + "\')\";\">Template 3D structure</a>"
      links += generateLink(pdbBaseLink, idTrimmed, "PDB")
      links += generateLink(ncbiBaseLink, idTrimmed, "NCBI")
    } else if (db == "mmcif") {
      links += "<a data-open=\"structureModal\" onclick=\"showStructure(\'" + id + "\')\";\">Template 3D structure</a>"
      links += generateLink(pdbeBaseLink, idPdb, "PDBe")
    } else if (db == "pfam") {
      idCDD = idCDD.replaceAll("\\..*", "")
      links += generateLink(cddBaseLink, idCDD, "CDD")
    } else if (db == "ncbi") {
      links += generateLink(ncbiProteinBaseLink, idNcbi, "NCBI Fasta")
    }

    Html(links.mkString(" | "))
  }

  def getLinksHmmer(id: String): Html = {
    val db = identifyDatabase(id)
    var links = new ArrayBuffer[String]()
    var idNcbi = id.replaceAll("#", ".") + "?report=fasta"
    if (db == "ncbi") {
      links += generateLink(ncbiProteinBaseLink, idNcbi, "NCBI Fasta")
    }
    Html(links.mkString(" | "))
  }

  def generateLink(baseLink: String, id: String, name: String): String =
    "<a href='" + baseLink + id + "' target='_blank'>" + name + "</a>"

  def identifyDatabase(id: String): String = id match {
    case scopReg(_) => "scop"
    case mmcifShortReg(_) => "mmcif"
    case mmcifReg(_) => "mmcif"
    case prkReg(_) => "prk"
    case ncbiCDReg(_) => "ncbicd"
    case cogkogReg(_) => "cogkog"
    case tigrReg(_) => "tigr"
    case smartReg(_) => "smart"
    case pfamReg(_, _) => "pfam"
    case uniprotReg(_) => "uniprot"
    case ncbiReg(_) => "ncbi"

    case e: String => Logger.info("Struc: (" + e + ") could not be matched against any database!"); ""
  }

  def percentage(str: String): String = {
    val num = str.toDouble
    val percent = (num * 100).toInt.toString + "%"
    percent
  }

  def calculatePercentage(num1_ : Int, num2_ : Int): String = {
    val num1 = num1_.toDouble
    val num2 = num2_.toDouble
    val percent = ((num1 / num2) * 100).toInt.toString + "%"
    percent
  }

  def wrapSequence(seq: String, num: Int): String = {
    var seqWrapped = ""
    for {i <- 0 to seq.length if i % num == 0} if (i + num < seq.length) {
      seqWrapped += "<tr><td></td><td class=\"sequence\">" + seq.slice(i, (i + num)) + "</td></tr>"
    } else {
      seqWrapped += "<tr><td></td><td class=\"sequence\">" + seq.substring(i) + "</td></tr>"
    }

    seqWrapped
  }

  def getCheckbox(num: Int): String = {
    "<input type=\"checkbox\" value=\"" + num + "\" name=\"alignment_elem\" class=\"checkbox\"><a onclick=\"scrollToElem(" + num + ")\">" + num + "</a>"
  }

  def getAddScrollLink(num: Int): String = {
    "<a onclick=\"scrollToElem(" + num + ")\">" + num + "</a>"
  }

  def addBreak(description: String): String = {
    description.replaceAll("(\\S{40})", "$1</br>");
  }

  def insertMatch(seq: String, length: Int, hitArr: List[Int]): String = {
    var newSeq = ""
    for (starPos <- hitArr) {
      val endPos = starPos + length
      newSeq += seq.slice(0, starPos) + "<span class=\"patternMatch\">" + seq.slice(starPos, endPos) + "</span>" + seq
        .substring(endPos)

    }
    newSeq.replaceAll("""\s""", "")
    newSeq
  }

  def clustal(alignment: AlignmentResult, begin: Int, breakAfter: Int, color: Boolean): String = {
    if (begin >= alignment.alignment.head.seq.length) {
      return ""
    } else {
      var string = alignment.alignment.map { elem =>
        "<tr>" +
          "<td>" +
          "<input type=\"checkbox\" value=\"" + elem.num + "\" name=\"alignment_elem\" class=\"checkbox\"><b>" +
          "</b><td>" +
          "<b><span class='clustalAcc'>" + elem.accession.take(20) + "</span></b><br />" +
          "</td>" +
          "</td>" +
          "<td class=\"sequence\">" + {
          if (color) colorRegexReplacer(elem.seq.slice(begin, Math.min(begin + breakAfter, elem.seq.length))) else elem.seq.slice(begin, Math.min(begin + breakAfter, elem.seq.length))
        } +
          "</td>" +
          "</tr>"
      }
        return {
          string.mkString + "<tr class=\"blank_row\"><td colspan=\"3\"></td></tr><tr class=\"blank_row\"><td colspan=\"3\"></td></tr>" + clustal(alignment, begin + breakAfter, breakAfter, color)
        }
    }
  }

  def hmmerHitWrapped(hit: HmmerHSP, charCount: Int, breakAfter: Int, beginQuery: Int, beginTemplate: Int): String ={
    if (charCount >= hit.hit_len){
      return ""
    }
    else {
      val query = hit.query_seq.slice(charCount, Math.min(charCount + breakAfter, hit.query_seq.length))
      val midline = hit.midline.slice(charCount, Math.min(charCount + breakAfter, hit.midline.length))
      val template = hit.hit_seq.slice(charCount, Math.min(charCount + breakAfter, hit.hit_seq.length))
      val queryEnd = lengthWithoutDashDots(query)
      val templateEnd = lengthWithoutDashDots(template)
      if (beginQuery == beginQuery + queryEnd) {
        return ""
      } else {
        return {
          "<tr class='sequence'><td></td><td>Q " + (beginQuery + 1) + "</td><td>" + query + "   " + (beginQuery + queryEnd) + "</td></tr>" +
            "<tr class='sequence'><td></td><td></td><td>" + midline + "</td></tr>" +
            "<tr class='sequence'><td></td><td>T " + (beginTemplate  + 1) + "</td><td>" + template + "   " + (beginTemplate + templateEnd) + "</td></tr>" +
            "<tr class=\"blank_row\"><td colspan=\"3\"></td></tr>" + "<tr class=\"blank_row\"><td colspan=\"3\"></td></tr>" +
            hmmerHitWrapped(hit, charCount + breakAfter, breakAfter, beginQuery + queryEnd, beginTemplate + templateEnd)
        }
      }
    }
  }

  def psiblastHitWrapped(hit: PSIBlastHSP, charCount: Int, breakAfter: Int, beginQuery: Int, beginTemplate: Int): String ={
    if (charCount >= hit.hit_len){
      return ""
    }
    else {
      val query = hit.query_seq.slice(charCount, Math.min(charCount + breakAfter, hit.query_seq.length))
      val midline = hit.midline.slice(charCount, Math.min(charCount + breakAfter, hit.midline.length))
      val template = hit.hit_seq.slice(charCount, Math.min(charCount + breakAfter, hit.hit_seq.length))
      val queryEnd = lengthWithoutDashDots(query)
      val templateEnd = lengthWithoutDashDots(template)
      if (beginQuery == beginQuery + queryEnd) {
        return ""
      } else {
        return {
          "<tr class='sequence'><td></td><td>Q " + beginQuery + "</td><td>" + query + "  " + (beginQuery + queryEnd - 1) + "</td></tr>" +
            "<tr class='sequence'><td></td><td></td><td>" + midline + "</td></tr>" +
            "<tr class='sequence'><td></td><td>T " + beginTemplate + "</td><td>" + template + "  " + (beginTemplate + templateEnd - 1) + "</td></tr>" +
            "<tr class=\"blank_row\"><td colspan=\"3\"></td></tr>" + "<tr class=\"blank_row\"><td colspan=\"3\"></td></tr>" +
            psiblastHitWrapped(hit, charCount + breakAfter, breakAfter, beginQuery + queryEnd, beginTemplate + templateEnd)
        }
      }
    }
  }


  def lengthWithoutDashDots(str : String): Int ={
    str.length-str.count(char =>  char == '-' ||  char == ".")
  }

  def hhblitsHitWrapped(hit: HHBlitsHSP, charCount: Int, breakAfter: Int, beginQuery: Int, beginTemplate: Int): String ={
    if (charCount >= hit.length){
      return ""
    }
    else {
      val query = hit.query.seq.slice(charCount, Math.min(charCount + breakAfter, hit.query.seq.length))
      val queryCons = hit.query.consensus.slice(charCount, Math.min(charCount + breakAfter, hit.query.consensus.length))
      val midline = hit.agree.slice(charCount, Math.min(charCount + breakAfter, hit.agree.length))
      val templateCons = hit.template.consensus.slice(charCount, Math.min(charCount + breakAfter, hit.template.consensus.length))
      val template = hit.template.seq.slice(charCount, Math.min(charCount + breakAfter, hit.template.seq.length))
      val queryEnd = lengthWithoutDashDots(query)
      val templateEnd = lengthWithoutDashDots(template)
      if (beginQuery == beginQuery + queryEnd) {
        return ""
      } else {
        return {
          "<tr class='sequence'><td></td><td>Q " +  hit.query.accession + "</td><td>" + beginQuery + "</td><td>" + query + "  " + (beginQuery + queryEnd - 1) + " (" + hit.query.ref + ")" + "</td></tr>" +
            "<tr class='sequence'><td></td><td>Q Consensus " + "</td><td>" + beginQuery + "</td><td>" + queryCons + "  " + (beginQuery + queryEnd - 1) + " (" + hit.query.ref + ")" + "</td></tr>" +
            "<tr class='sequence'><td></td><td></td><td></td><td>" + midline + "</td></tr>" +
            "<tr class='sequence'><td></td><td>T Consensus " + "</td><td>" + beginTemplate + "</td><td>" + templateCons + "  " + (beginTemplate + templateEnd - 1) + " (" + hit.template.ref + ")" + "</td></tr>" +
            "<tr class='sequence'><td></td><td>T " + hit.template.accession + "</td><td>" + beginTemplate + "</td><td>" + template + "  " + (beginTemplate + templateEnd - 1) + " (" + hit.template.ref + ")" + "</td></tr>" +
            "<tr class=\"blank_row\"><td colspan=\"3\"></td></tr>" + "<tr class=\"blank_row\"><td colspan=\"3\"></td></tr>" +
            hhblitsHitWrapped(hit, charCount + breakAfter, breakAfter, beginQuery + queryEnd, beginTemplate + templateEnd)
        }
      }
    }
  }

  def hhpredHitWrapped(hit: HHPredHSP, charCount: Int, breakAfter: Int, beginQuery: Int, beginTemplate: Int, color: Boolean): String ={
    if (charCount >= hit.length){
      return ""
    }
    else {
      val querySSDSSP = hit.query.ss_dssp.slice(charCount, Math.min(charCount + breakAfter, hit.query.ss_dssp.length))
      val querySSPRED = hit.query.ss_pred.slice(charCount, Math.min(charCount + breakAfter, hit.query.ss_pred.length))
      val query = hit.query.seq.slice(charCount, Math.min(charCount + breakAfter, hit.query.seq.length))
      val queryCons = hit.query.consensus.slice(charCount, Math.min(charCount + breakAfter, hit.query.consensus.length))
      val midline = hit.agree.slice(charCount, Math.min(charCount + breakAfter, hit.agree.length))
      val templateCons = hit.template.consensus.slice(charCount, Math.min(charCount + breakAfter, hit.template.consensus.length))
      val template = hit.template.seq.slice(charCount, Math.min(charCount + breakAfter, hit.template.seq.length))
      val templateSSDSSP = hit.template.ss_dssp.slice(charCount, Math.min(charCount + breakAfter, hit.template.ss_dssp.length))
      val templateSSPRED = hit.template.ss_pred.slice(charCount, Math.min(charCount + breakAfter, hit.template.ss_pred.length))
      val confidence = hit.confidence.slice(charCount, Math.min(charCount + breakAfter, hit.confidence.length))
      val queryEnd = lengthWithoutDashDots(query)
      val templateEnd = lengthWithoutDashDots(template)

      if (beginQuery == beginQuery + queryEnd) {
        return ""
      } else {

          var html = ""
          if(!querySSPRED.isEmpty) {
          html +=" <tr class='sequence'><td></td><td>Q ss_pred" + "</td><td>" + "</td><td>" + BlastVisualization.SSColorReplace(querySSPRED) + "</td></tr>"
          }
          if(!querySSDSSP.isEmpty) {
            html += "<tr class='sequence'><td></td><td>Q ss_dssp" + "</td><td>" + "</td><td>" + BlastVisualization.SSColorReplace(querySSDSSP) + "</td></tr>"
          }
        html +="<tr class='sequence'><td></td><td>Q " +  hit.query.accession + "</td><td>" + beginQuery + "</td><td>" + {if(color) colorRegexReplacer(query) else query} + "  " + (beginQuery + queryEnd - 1) + " (" + hit.query.ref + ")" + "</td></tr>" +
         "<tr class='sequence'><td></td><td>Q Consensus " + "</td><td>" + beginQuery + "</td><td>" + queryCons + "  " + (beginQuery + queryEnd - 1) + " (" + hit.query.ref + ")" + "</td></tr>" +
            "<tr class='sequence'><td></td><td></td><td></td><td>" + midline + "</td></tr>" +
            "<tr class='sequence'><td></td><td>T Consensus " + "</td><td>" + beginTemplate + "</td><td>" + templateCons + "  " + (beginTemplate + templateEnd - 1) + " (" + hit.template.ref + ")" + "</td></tr>" +
            "<tr class='sequence'><td></td><td>T " + hit.template.accession + "</td><td>" + beginTemplate + "</td><td>" + {if(color) colorRegexReplacer(template) else template} + "  " + (beginTemplate + templateEnd - 1) + " (" + hit.template.ref + ")" + "</td></tr>"
        if(!templateSSDSSP.isEmpty) {
          html += "<tr class='sequence'><td></td><td>T ss_dssp" + "</td><td>" + "</td><td>" + BlastVisualization.SSColorReplace(templateSSDSSP) + "</td></tr>"
        }
        if(!templateSSPRED.isEmpty) {
          html +=" <tr class='sequence'><td></td><td>T ss_pred" + "</td><td>" + "</td><td>" + BlastVisualization.SSColorReplace(templateSSPRED) + "</td></tr>"
        }
        if(!confidence.isEmpty) {
          html +=" <tr class='sequence'><td></td><td>Confidence" + "</td><td>" + "</td><td>" + confidence + "</td></tr>"
        }

        html += "<tr class=\"blank_row\"><td colspan=\"3\"></td></tr>" + "<tr class=\"blank_row\"><td colspan=\"3\"></td></tr>"

            return html + hhpredHitWrapped(hit, charCount + breakAfter, breakAfter, beginQuery + queryEnd, beginTemplate + templateEnd, color)
      }
    }
  }


  def hhompHitWrapped(hit: HHompHSP, charCount: Int, breakAfter: Int, beginQuery: Int, beginTemplate: Int, color: Boolean): String ={
    if (charCount >= hit.length){
      return ""
    }
    else {
      val querySSCONF = hit.query.ss_conf.slice(charCount, Math.min(charCount + breakAfter, hit.query.ss_conf.length))
      val querySSDSSP = hit.query.ss_dssp.slice(charCount, Math.min(charCount + breakAfter, hit.query.ss_dssp.length))
      val querySSPRED = hit.query.ss_pred.slice(charCount, Math.min(charCount + breakAfter, hit.query.ss_pred.length))
      val query = hit.query.seq.slice(charCount, Math.min(charCount + breakAfter, hit.query.seq.length))
      val queryCons = hit.query.consensus.slice(charCount, Math.min(charCount + breakAfter, hit.query.consensus.length))
      val midline = hit.agree.slice(charCount, Math.min(charCount + breakAfter, hit.agree.length))
      val templateCons = hit.template.consensus.slice(charCount, Math.min(charCount + breakAfter, hit.template.consensus.length))
      val template = hit.template.seq.slice(charCount, Math.min(charCount + breakAfter, hit.template.seq.length))
      val templateSSDSSP = hit.template.ss_dssp.slice(charCount, Math.min(charCount + breakAfter, hit.template.ss_dssp.length))
      val templateSSPRED = hit.template.ss_pred.slice(charCount, Math.min(charCount + breakAfter, hit.template.ss_pred.length))
      val templateSSCONF = hit.template.ss_conf.slice(charCount, Math.min(charCount + breakAfter, hit.template.ss_conf.length))
      val templateBBPRED = hit.template.bb_pred.slice(charCount, Math.min(charCount + breakAfter, hit.template.bb_pred.length))
      val templateBBCONF = hit.template.bb_conf.slice(charCount, Math.min(charCount + breakAfter, hit.template.bb_conf.length))

      val queryEnd = lengthWithoutDashDots(query)
      val templateEnd = lengthWithoutDashDots(template)

      if (beginQuery == beginQuery + queryEnd) {
        return ""
      } else {

        var html = ""

        if(!querySSCONF.isEmpty) {
          html +=" <tr class='sequence'><td></td><td>Q ss_conf" + "</td><td>" + "</td><td>" + querySSCONF + "</td></tr>"
        }
        if(!querySSPRED.isEmpty) {
          html +=" <tr class='sequence'><td></td><td>Q ss_pred" + "</td><td>" + "</td><td>" + BlastVisualization.SSColorReplace(querySSPRED) + "</td></tr>"
        }
        if(!querySSDSSP.isEmpty) {
          html += "<tr class='sequence'><td></td><td>Q ss_dssp" + "</td><td>" + "</td><td>" + BlastVisualization.SSColorReplace(querySSDSSP) + "</td></tr>"
        }
        html +="<tr class='sequence'><td></td><td>Q " +  hit.query.accession + "</td><td>" + beginQuery + "</td><td>" + {if(color) colorRegexReplacer(query) else query} + "  " + (beginQuery + queryEnd - 1) + " (" + hit.query.ref + ")" + "</td></tr>" +
          "<tr class='sequence'><td></td><td>Q Consensus " + "</td><td>" + beginQuery + "</td><td>" + queryCons + "  " + (beginQuery + queryEnd - 1) + " (" + hit.query.ref + ")" + "</td></tr>" +
          "<tr class='sequence'><td></td><td></td><td></td><td>" + midline + "</td></tr>" +
          "<tr class='sequence'><td></td><td>T Consensus " + "</td><td>" + beginTemplate + "</td><td>" + templateCons + "  " + (beginTemplate + templateEnd - 1) + " (" + hit.template.ref + ")" + "</td></tr>" +
          "<tr class='sequence'><td></td><td>T " + hit.template.accession + "</td><td>" + beginTemplate + "</td><td>" + {if(color) colorRegexReplacer(template) else template} + "  " + (beginTemplate + templateEnd - 1) + " (" + hit.template.ref + ")" + "</td></tr>"
        if(!templateSSDSSP.isEmpty) {
          html += "<tr class='sequence'><td></td><td>T ss_dssp" + "</td><td>" + "</td><td>" + BlastVisualization.SSColorReplace(templateSSDSSP) + "</td></tr>"
        }
        if(!templateSSPRED.isEmpty) {
          html +=" <tr class='sequence'><td></td><td>T ss_pred" + "</td><td>" + "</td><td>" + BlastVisualization.SSColorReplace(templateSSPRED) + "</td></tr>"
        }
        if(!templateSSCONF.isEmpty) {
          html +=" <tr class='sequence'><td></td><td>T ss_conf" + "</td><td>" + "</td><td>" + templateSSCONF + "</td></tr>"
        }
        if(!templateBBPRED.isEmpty) {
          html +=" <tr class='sequence'><td></td><td>T bb_pred" + "</td><td>" + "</td><td>" + templateBBPRED + "</td></tr>"
        }
        if(!templateBBCONF.isEmpty) {
          html +=" <tr class='sequence'><td></td><td>T bb_conf" + "</td><td>" + "</td><td>" + templateBBCONF + "</td></tr>"
        }

        html += "<tr class=\"blank_row\"><td colspan=\"3\"></td></tr>" + "<tr class=\"blank_row\"><td colspan=\"3\"></td></tr>"

        return html + hhompHitWrapped(hit, charCount + breakAfter, breakAfter, beginQuery + queryEnd, beginTemplate + templateEnd, color)
      }
    }
  }

  def quick2dWrapped(result: Quick2DResult, charCount: Int, breakAfter: Int): String = {
    val length = result.query.seq.length
    if (charCount >= length){
      return ""
    }
    else {
      var htmlString = ""
      val query = result.query.seq.slice(charCount, Math.min(charCount + breakAfter, result.query.seq.length))
      val psipred = result.psipred.seq.slice(charCount, Math.min(charCount + breakAfter, result.query.seq.length))
      val marcoil = result.marcoil.seq.slice(charCount, Math.min(charCount + breakAfter, result.query.seq.length))
      val coils = result.coils.seq.slice(charCount, Math.min(charCount + breakAfter, result.query.seq.length))
      val pcoils = result.pcoils.seq.slice(charCount, Math.min(charCount + breakAfter, result.query.seq.length))
      val tmhmm = result.tmhmm.seq.slice(charCount, Math.min(charCount + breakAfter, result.query.seq.length))
      val phobius = result.phobius.seq.slice(charCount, Math.min(charCount + breakAfter, result.query.seq.length))
      val polyphobius = result.polyphobius.seq.slice(charCount, Math.min(charCount + breakAfter, result.query.seq.length))
      val spider2 = result.spider2.seq.slice(charCount, Math.min(charCount + breakAfter, result.query.seq.length))
      val spotd = result.spotd.seq.slice(charCount, Math.min(charCount + breakAfter, result.query.seq.length))
      val iupred = result.iupred.seq.slice(charCount, Math.min(charCount + breakAfter, result.query.seq.length))

      htmlString += "<tr class='sequenceCompact sequenceBold'><td>&nbsp;&nbsp;&nbsp;AA_QUERY</td><td>"+(charCount + 1)+"</td><td>"+query+"&nbsp;&nbsp;&nbsp;&nbsp;"+"<td>"+Math.min(length,charCount+breakAfter)+"</td>"
      htmlString += "<tr class='sequenceCompact'><td>&nbsp;&nbsp;&nbsp;SS_"+result.psipred.name.toUpperCase()+"</td><td></td><td>"+this.Q2DColorReplace(result.psipred.name, psipred.replace("C", "&nbsp;"))+"</td>"
      if(!spider2.isEmpty) {
        htmlString += "<tr class='sequenceCompact'><td>&nbsp;&nbsp;&nbsp;SS_" + result.spider2.name.toUpperCase() + "</td><td></td><td>" + this.Q2DColorReplace(result.spider2.name, spider2.replace("C","&nbsp;")) + "</td>"
      }
      if(!marcoil.isEmpty) {
        htmlString += "<tr class='sequenceCompact'><td>&nbsp;&nbsp;&nbsp;CC_" + result.marcoil.name.toUpperCase() + "</td><td></td><td>" + this.Q2DColorReplace(result.marcoil.name, marcoil.replace("x","&nbsp;")) + "</td>"
      }
      if(!coils.isEmpty) {
        htmlString += "<tr class='sequenceCompact'><td>&nbsp;&nbsp;&nbsp;CC_" + result.coils.name.toUpperCase() + "_W28</td><td></td><td>" + this.Q2DColorReplace(result.coils.name, coils.replace("x","&nbsp;")) + "</td>"
      }
      if(!pcoils.isEmpty) {
        htmlString += "<tr class='sequenceCompact'><td>&nbsp;&nbsp;&nbsp;CC_" + result.pcoils.name.toUpperCase() + "_W28</td><td></td><td>" + this.Q2DColorReplace(result.pcoils.name, pcoils.replace("x","&nbsp;")) + "</td>"
      }
      if(!tmhmm.isEmpty) {
        htmlString += "<tr class='sequenceCompact'><td>&nbsp;&nbsp;&nbsp;TM_" + result.tmhmm.name.toUpperCase() + "</td><td></td><td>" + this.Q2DColorReplace(result.tmhmm.name, tmhmm.replace("x","&nbsp;")) + "</td>"
      }
      if(!phobius.isEmpty) {
        htmlString += "<tr class='sequenceCompact'><td>&nbsp;&nbsp;&nbsp;TM_" + result.phobius.name.toUpperCase() + "</td><td></td><td>" + this.Q2DColorReplace(result.phobius.name, phobius.replace("x","&nbsp;")) + "</td>"
      }
      if(!polyphobius.isEmpty) {
        htmlString += "<tr class='sequenceCompact'><td>&nbsp;&nbsp;&nbsp;TM_" + result.polyphobius.name.toUpperCase() + "</td><td></td><td>" + this.Q2DColorReplace(result.polyphobius.name, polyphobius.replace("x","&nbsp;")) + "</td>"
      }
      if(!spotd.isEmpty) {
        htmlString += "<tr class='sequenceCompact'><td>&nbsp;&nbsp;&nbsp;DO_" + result.spotd.name.toUpperCase() + "</td><td></td><td>" + this.Q2DColorReplace(result.spotd.name, spotd.replace("O","&nbsp;")) + "</td>"
      }
      if(!iupred.isEmpty) {
        htmlString += "<tr class='sequenceCompact'><td>&nbsp;&nbsp;&nbsp;DO_" + result.iupred.name.toUpperCase() + "</td><td></td><td>" + this.Q2DColorReplace(result.iupred.name, iupred.replace("O","&nbsp;")) + "</td>"
      }

      htmlString += "<tr class=\"blank_row\"><td colspan=\"4\"></td></tr>" + "<tr class=\"blank_row\"><td colspan=\"4\"></td></tr>"  + "<tr class=\"blank_row\"><td colspan=\"4\"></td></tr>"  + "<tr class=\"blank_row\"><td colspan=\"4\"></td></tr>" + "<tr class=\"blank_row\"><td colspan=\"4\"></td></tr>"
      return htmlString + quick2dWrapped(result, charCount + breakAfter, breakAfter)
      }
    }
}
