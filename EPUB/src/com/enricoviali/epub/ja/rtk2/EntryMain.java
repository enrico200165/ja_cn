package com.enricoviali.epub.ja.rtk2;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.enrico_viali.jacn.common.CJKUtils;

public class EntryMain {

    public EntryMain(EPUB_main epubPar, int typePar) {
        selectors = new Selectors();
        this.epub = epubPar;
        this.entryType = typePar;
        reset();
    }

    void reset() {

        fillManually = false;

        tableID = null;
        capitolo = null;
        tableID = null;
        // --- Kanji1
        kanji = null;
        signPrim = null;
        readings = null;
        link = null;
        RTK1Frame = Defs.INT_UNSET;
        // --- Kanji2
        kanji2 = null;
        signPrim2 = null;
        readings2 = null;
        link2 = null;
        RTK1Frame2 = Defs.INT_UNSET;
        // --- RFrame
        setRTK2Frame(-1);
        compKanj = null;
        compReading = null;
        compMean = null;
        // Commento
        comment = null;

    }

    public String calculateCapitolo() {

        if (getRTK2Frame() <= 56)
            return "01_0_kana_their_kanji";

        if (getRTK2Frame() <= 195)
            return "02_1_pure_groups_easy";
        if (getRTK2Frame() <= 336)
            return "02_2_pure_groups_3";
        if (getRTK2Frame() <= 574)
            return "02_3_pure_groups_2";

        if (getRTK2Frame() <= 623)
            return "03_1t_chinese_read";

        if (getRTK2Frame() <= 735)
            return "04_no_chinese_read";

        if (getRTK2Frame() <= 1026)
            return "05_semi_pure";

        if (getRTK2Frame() <= 1267)
            return "06_everyday_words";

        if (getRTK2Frame() <= 1257)
            return "06_everyday_words";

        if (getRTK2Frame() <= 1269)
            return "07_1_mixed_groups";
        if (getRTK2Frame() <= 1305)
            return "07_2_mixed_groups";
        if (getRTK2Frame() <= 1394)
            return "07_3_mixed_groups_2exc";
        if (getRTK2Frame() <= 1669)
            return "07_4_mixed_groups_rem";

        if (getRTK2Frame() <= 1918)
            return "08_everyday_words";

        if (getRTK2Frame() <= 2071)
            return "09_potpourri";

        if (getRTK2Frame() <= 2409)
            return "10_supplementary";

        // 09_potpourri

        // log.error(getRTK2Frame());

        return "error";
    }

    public String getCapitolo() {
        return this.capitolo;
    }

    public String setCapitolo(String cap) {
        if (cap != null) {
            this.capitolo = cap;
        } else {
            this.capitolo = calculateCapitolo();
        }
        return "error";
    }

    public boolean isValid(int type) {
        boolean ret = true;

        if (getRTK2Frame() < 1) {
            log.error(epub.curCoordinates() + " invalid RTK2 frame: " + getRTK2Frame() + " kanji:" + getKanji());
            // System.exit(1);
            return false;
        }
        /* fa crashare il programma
        if (getRTK2Frame() != (epub.getPreviousRTK2Frame()+1)) {
            log.error(epub.curCoordinates()+" invalid RTK2 frame: " + getRTK2Frame() + " kanji:" + getKanji());
            // System.exit(1);
            return false;
        }
        */

        if (!(this.entryType == Defs.ENTRY_TYPE_MAIN || this.entryType == Defs.ENTRY_TYPE_MNE_KUN
        || this.entryType == Defs.ENTRY_TYPE_NO_ON)) {
            log.error(epub.curCoordinates() + " invalid entry type: " + this.entryType);
            return false;
        }

        if (fillManually)
            return true;

        // --- kanji field ---
        if (getKanji() == null) {
            log.error("null kanji");
            return false;
        }
        if (!CJKUtils.isAllEastern(getKanji(), true)) {
            log.error(epub.curCoordinates() + " invalid kanji: " + getKanji() + " kanji:" + getKanji());
            // System.exit(1);
            return false;
        }
        if ((this.entryType == Defs.ENTRY_TYPE_MAIN)) {
            if ((getKanji().length() != 1) || !CJKUtils.isAllKanji(getKanji(), false)) {
                log.error(epub.curCoordinates()+" invalid kanji: " + getKanji() + " kanji:" + getKanji());
                return false;
            }
        }

        // --- signal primitive ---
        if (getSignPrim() != null && getSignPrim().length() > 0) {
            if (getSignPrim().length() != 1) {
                log.error(epub.curCoordinates()+" signal primitive wrong length: "+getSignPrim().length()+" "+getSignPrim());
                return false;
            }
            if (!CJKUtils.isAllEastern(getSignPrim(), false)) {
                log.error(epub.curCoordinates()+" signal primitive: bad characters: "+getSignPrim());
                return false;
            }
        }

        if (capitolo == null || capitolo.length() < 4) {
            log.error(epub.curCoordinates()+" capitolo invalid : " + getCapitolo());
            return false;
        }

        if (!ret) {
            log.warn(epub.curCoordinates()+" entry not valid"+"\n"+toString());
            log.info("brakpoint hook");
            // qui sarebbe il caso di fare un dump della tabella, dovrei inserire un puntatore che però potrebbe essere nulll  log.warn("");
        }
        return ret;
    }

    void setComment(String com) {
        this.comment = com;
    }

    public String toString() {
        String sep = "  ";
        String eq = "=";
        String s = "";


        s += sep + "kanji" + eq + kanji;
        s += sep + "signal primitive" + eq + signPrim;
        s += sep + "On" + eq + readings;
        s += sep + "link" + eq + link;
        s += sep + "RTK1Frame" + eq + RTK1Frame;

        s += sep + "RTK2Frame" + eq + getRTK2Frame();

        
        s += sep + "table" + eq + getTableID();
        s += sep + "capitolo";
        // da riga 2

        return s;
    }

    public boolean processRigaKanji1(boolean overwrite, Element rigaKanji1) {

        if (rigaKanji1 == null) {
            log.error(epub.getCurTableFile() + " " + tableID + " riga kanji1 is null");
            return false;
        }

        Elements TDs = rigaKanji1.select("td");
        int pos = TDs.size();

        // --- simply check size
        Elements nonEmpty = new Elements();
        for (Element e : TDs) {
            if (e.text().length() > 0)
                nonEmpty.add(e);
        }
        if (nonEmpty.size() < 3) {
            log.error(epub.getCurTableFile() + " " + tableID + " riga kanji 1 con elementi mancanti\n" + rigaKanji1.html());
            return false;
        }

        // -- real processing
        pos--; // usually 4
        if (overwrite || getRTK1Frame() <= 0) {
            Element RTK1FrameE = TDs.get(pos);
            setRTK1Frame(Integer.parseInt(RTK1FrameE.text()));
        }

        pos--; // 3
        if (overwrite || getLink() == null) {
            Element linkE = TDs.get(pos);
            link = linkE.text();
        }

        pos--; // 2
        if (overwrite || getReadings() == null) {
            Element OnE = TDs.get(pos);
            this.setReadings(OnE.text());
        }

        pos--; // 1
        if (overwrite || getSignPrim() == null) {
            Element signPrimE = TDs.get(pos);
            setSignPrim(signPrimE.text());
        }

        pos--; // 0
        if (overwrite || getKanji() == null) {
            Element kanjiE = TDs.get(pos);
            setKanji(kanjiE.text());
        }

        return true;
    }

    public boolean processRigaKanji2(boolean overwrite, Element rigaKanji2) {

        if (rigaKanji2 == null)
            return false;

        Elements TDs = rigaKanji2.select("td");
        int pos = TDs.size();

        pos--; // usually 4
        if (pos < 0) {
            log.warn("");
            return false;
        }
        if (overwrite || getRTK1Frame() == -1) {
            Element RTK1FrameE = TDs.get(pos);
            setRTK1Frame2(Integer.parseInt(RTK1FrameE.text()));
        }

        pos--; // 3
        if (pos < 0) {
            log.warn("");
            return false;
        }
        if (overwrite || getLink2() == null) {
            Element linkE = TDs.get(pos);
            setLink2(linkE.text());
        }

        pos--; // 2
        if (pos < 0) {
            log.warn("");
            return false;
        }
        if (overwrite || getReadings2() == null) {
            Element OnE = TDs.get(pos);
            setReadings2(OnE.text());
        }

        pos--; // 1
        if (pos < 0) {
            log.warn("");
            return false;
        }
        if (overwrite || getSignPrim2() == null) {
            Element signPrimE = TDs.get(pos);
            setSignPrim2(signPrimE.text());
        }

        pos--; // 0
        if (pos < 0) {
            log.warn("");
            return false;
        }
        if (overwrite || getKanji2() == null) {
            Element kanjiE = TDs.get(pos);
            setKanji2(kanjiE.text());
        }

        return true;
    }

    public String getCompKanj() {
        return compKanj;
    }

    public void setCompKanj(String compKanj) {
        this.compKanj = compKanj;
    }

    public String getCompReading() {
        return compReading;
    }

    public void setCompReading(String compReading) {
        this.compReading = compReading;
    }

    public String getCompMean() {
        return compMean;
    }

    public void setCompMean(String compMean) {
        this.compMean = compMean;
    }

    public String getComment() {
        return comment;
    }

    public void setRTK1Frame(int rTK1FramePar) {
        RTK1Frame = rTK1FramePar;
    }

    public void setRTK2Frame(int rTK2Frame) {
        RTK2Frame = rTK2Frame;
        capitolo = calculateCapitolo();
    }

    boolean processRigaRFrameCompo(boolean overwrite, Element rigaRFrame) {

        if (rigaRFrame == null)
            return false;

        Elements TDs = rigaRFrame.select("td");
        int pos = TDs.size();
        Element cur;

        // --- simply check size
        Elements nonEmpty = new Elements();
        for (Element e : TDs) {
            if (e.text().length() > 0)
                nonEmpty.add(e);
        }
        if (nonEmpty.size() < 3) {
            if (!splitTable(Utils.nrFromFName(epub.getCurTableFile()), Utils.tableNr(tableID)))
                log.warn(epub.getCurTableFile() + " " + epub.getCurTableNr() + " riga RFrame con elementi mancanti\n" + rigaRFrame.html());
            return false;
        }

        // -- real processing

        pos--; // usually 4

        pos--; // usually 3
        if (pos < 0) {
            log.warn(epub.getCurTableFile() + " " + epub.getCurTableNr() + " negative pos");
            return false;
        }
        if (overwrite || getCompMean() == null) {
            cur = TDs.get(pos);
            setCompMean(cur.text());
        }

        pos--; // usually 2
        if (pos < 0) {
            log.warn(epub.getCurTableFile() + " " + epub.getCurTableNr() + " negative pos");
            return false;
        }
        if (overwrite || getCompReading() == null) {
            cur = TDs.get(pos);
            setCompReading(cur.text());
        }

        pos--; // usually 1
        if (pos < 0) {
            log.warn(epub.getCurTableFile() + " " + epub.getCurTableNr() + " negative pos");
            return false;
        }
        if (overwrite || getCompKanj() == null) {
            cur = TDs.get(pos);
            setCompKanj(cur.text());
        }

        pos--; // usually 0
        if (pos < 0) {
            log.warn(epub.getCurTableFile() + " " + epub.getCurTableNr() + " negative pos");
            return false;
        }
        if (overwrite || getRTK2Frame() == -1) {
            Element RTK2FrameE = TDs.get(pos);
            setRTK2Frame(RTK2FrameE.text());
        }
        return true;
    }

    boolean processRigaCompo2(boolean overwrite, Element rigaCompo2) {

        if (rigaCompo2 == null)
            return false;

        Elements TDs = rigaCompo2.select("td");
        int pos = TDs.size();
        Element cur;

        // --- simply check size
        Elements nonEmpty = new Elements();
        for (Element e : TDs) {
            if (e.text().length() > 0)
                nonEmpty.add(e);
        }
        if (nonEmpty.size() < 3) {
            if (!splitTable(Utils.nrFromFName(epub.getCurTableFile()), Utils.tableNr(tableID)))
                log.warn(epub.getCurTableFile() + " " + epub.getCurTableNr() + " riga RFrame con elementi mancanti\n" + rigaCompo2.html());
            return false;
        }

        // -- real processing

        pos--; // usually 4

        pos--; // usually 3
        if (pos < 0) {
            log.warn(epub.getCurTableFile() + " " + epub.getCurTableNr() + " negative pos");
            return false;
        }
        if (overwrite || getCompMean() == null) {
            cur = TDs.get(pos);
            setCompMean(cur.text());
        }

        pos--; // usually 2
        if (pos < 0) {
            log.warn(epub.getCurTableFile() + " " + epub.getCurTableNr() + " negative pos");
            return false;
        }
        if (overwrite || getCompReading() == null) {
            cur = TDs.get(pos);
            setCompReading(cur.text());
        }

        pos--; // usually 1
        if (pos < 0) {
            log.warn(epub.getCurTableFile() + " " + epub.getCurTableNr() + " negative pos");
            return false;
        }
        if (overwrite || getCompKanj() == null) {
            cur = TDs.get(pos);
            setCompKanj(cur.text());
        }

        pos--; // usually 0
        if (pos < 0) {
            log.warn(epub.getCurTableFile() + " " + epub.getCurTableNr() + " negative pos");
            return false;
        }
        if (overwrite || getRTK2Frame() == -1) {
            Element RTK2FrameE = TDs.get(pos);
            setRTK2Frame(RTK2FrameE.text());
        }
        return true;
    }

    boolean processRigaComment(boolean overwrite, Element rigaComm) {

        if (rigaComm == null)
            return true;

        Elements TDs = rigaComm.select("td");
        int pos = TDs.size();
        int nonEmptyTDs = 0;
        String comms = "";
        for (int i = 0; i < TDs.size(); i++) {
            if (TDs.get(i).text().length() > 0) {
                comms += " " + TDs.get(i).text();
                nonEmptyTDs++;
                if (i != 1)
                    log.debug(tableID + " comment at TD nr: " + i);
            }
        }
        if (overwrite || getComment() == null) {
            setComment(comms.trim());
        }

        if (nonEmptyTDs != 1) {
            log.error("commento sparso su più TD: " + nonEmptyTDs);
            return false;
        }
        return true;
    }

    /**
     * Dovrebbe elaborare solo i casi normali, e fallire se manca il minimo
     * @return
     */
    boolean processStandardEntry(TProcVars pv) {
        boolean ret = true;
        String tableID = pv.t.cssSelector();

        if (epub.getPreviousRTK2Frame() == 190)
            log.debug("breakpoint hook");

        if (!processRigaKanji1(true, pv.kanji1Row))
            ret = false;
        if (!processRigaKanji2(true, pv.kanji2Row)) {

        }
        if (!processRigaRFrameCompo(true, pv.RFrameRow)) {
            ret = false;
        }

        if (!processRigaComment(true, pv.CommentRow)) {
        }

        if (!ret && !splitTable(epub.getCurTableNr(), epub.getCurTableNr())) {
            log.error(epub.getCurTableFile() + " tableNr: " + epub.getCurTableNr() + " failed standard processing");
            ret = false;
        }
        log.debug("just for a brakpoint " + epub.getCurTableFile() + " tableNr: " + epub.getCurTableNr());

        return ret;
    }

    /**
     * pass to null rows not to process
     * @return 1 ok (primo passo) ma non completa
     * 2 completa e valida
     */
    int processAnomalyEntry(Element table) {
        boolean ret = true;
        String tableID = table.cssSelector();
        Elements rows = table.select(Defs.rowsSelector);

        switch (epub.getPreviousRTK2Frame()) {

            case 184: { // forse anche anomala
                if (epub.getfNumber() == 102) {
                    if (epub.getPass() == 0) {
                        processRigaKanji1(true, table.select("tr").first());
                        Element td = rows.get(1).select("td").get(0);
                        String tmp = td.text();
                        setRTK2Frame(tmp);
                        return 1;
                    } else {
                        log.error("should not be completing");
                        System.exit(1);
                    }
                } else if (epub.getfNumber() == 103) {
                    if (epub.getPass() == 1) {
                        setCompKanj(rows.get(0).select("td").get(0).text().trim());
                        setCompReading(rows.get(0).select("td").get(1).text().trim());
                        setCompMean(rows.get(0).select("td").get(2).text().trim());
                        return 2;
                    } else {
                        log.error("");
                    }
                }
                break;
            }

            case 190: { // normale, splittata
                if (epub.getfNumber() == 103) {
                    processRigaKanji1(true, table.select("tr").first());
                    return 1;
                } else if (epub.getfNumber() == 104) {
                    processRigaRFrameCompo(true, table.select("tr").first());
                    return 2;
                } else {
                    log.error("");
                    System.exit(1);
                }
                break;
            }

            case 315: { // normale, splittata
                if (epub.getfNumber() == 103) {
                    processRigaKanji1(true, table.select("tr").first());
                    return 1;
                } else if (epub.getfNumber() == 104) {
                    processRigaRFrameCompo(true, table.select("tr").first());
                    return 2;
                } else {
                    log.error("");
                    System.exit(1);
                }
                break;
            }

            case 444: {
                if (epub.getfNumber() == 104) {
                    processRigaKanji1(true, table.select("tr").first());
                    adHocFrameCompo(false,
                            table.select("tr").get(1).select("td").get(0).text() /* rframe2*/,
                            table.select("tr").get(1).select("td").get(1).text() /* compo */,
                            table.select("tr").get(1).select("td").get(2).text() /*reading*/,
                            null);
                    // adHocFrameCompo(table, false, R2Frame, compo, compoRead, compoMean);
                    return 1;
                } else if (epub.getfNumber() == 105 && epub.getPass() == 1) {
                    adHocFrameCompo(false,
                            null,
                            null,
                            null,
                            table.select("tr").get(0).select("td").get(0).text() /* rframe2*/
                    );
                    return 2;
                } else {
                    log.error("");
                }
                break;
            }

            case 740:
            case 746: {
                if (epub.getfNumber() == 202) {
                    adHocKanji1(table, false,
                            table.select("tr").get(0).select("td").get(1).text(), // kanji 
                            null /*sgnPrim*/,
                            table.select("tr").get(0).select("td").get(3).text()/*reading*/,
                            null /*lnkSel*/,
                            table.select("tr").get(0).select("td").get(5).text()/*R1FrSel*/);
                    adHocFrameCompo(false,
                            table.select("tr").get(1).select("td").get(1).text() /*R2FramePar*/,
                            table.select("tr").get(1).select("td").get(2).text() /*compoPar*/,
                            table.select("tr").get(1).select("td").get(3).text() /*compoReadPar*/,
                            table.select("tr").get(1).select("td").get(4).text() /*compoMeanPar*/
                    );
                    return 2;
                } else {
                    log.error("");
                }
                break;
            }
            case 754:
            case 760:
            case 765:
            case 771: {
                if (epub.getfNumber() == 202) {
                    adHocKanji1(table, false,
                            table.select("tr").get(0).select("td").get(1).text(), // kanji 
                            null /*sgnPrim*/,
                            table.select("tr").get(0).select("td").get(3).text()/*reading*/,
                            null /*lnkSel*/,
                            table.select("tr").get(0).select("td").get(5).text()/*R1FrSel*/);
                    processRigaRFrameCompo(false, table.select("tr").get(1));
                    return 2;
                } else {
                    log.error("");
                }
                break;
            }
            case 772: {
                if (epub.getfNumber() == 202) {

                    // ha due composti, su stessa riga, usa <p> diversi per dare graficamente illusione 2 righe
                    // preservo questa finzione anche se forse sarebbe meglio spezzare su due vere righe/sub-entries
                    adHocKanji1(table, false,
                            table.select("tr").get(0).select("td").get(1).text(), // kanji 
                            null /*sgnPrim*/,
                            table.select("tr").get(0).select("td").get(3).text()/*reading*/,
                            null /*lnkSel*/,
                            table.select("tr").get(0).select("td").get(5).text()/*R1FrSel*/);

                    setComment("ha due composti, su stessa riga, usa <p> diversi per dare graficamente illusione" +
                            " 2 righe. preservo questa finzione anche se forse sarebbe meglio spezzare su due vere "
                            + "righe/sub-entries");

                    setRTK2Frame(epub.getRTK2FrameFromTable(table));

                    setCompKanj(Utils.nlText(table.select("tr").get(1).select("td").get(2)));
                    setCompReading(Utils.nlText(table.select("tr").get(1).select("td").get(3)));
                    setCompMean(Utils.nlText(table.select("tr").get(1).select("td").get(4)));
                    return 2;
                } else {
                    log.error("");
                }
                break;
            }

            case 773: {
                if (epub.getfNumber() == 202) {
                    adHocKanji1(table, false,
                            table.select("tr").get(0).select("td").get(1).text(), // kanji 
                            null /*sgnPrim*/,
                            table.select("tr").get(0).select("td").get(3).text()/*reading*/,
                            null /*lnkSel*/,
                            table.select("tr").get(0).select("td").get(5).text()/*R1FrSel*/);
                    processRigaRFrameCompo(false, table.select("tr").get(1));
                    return 2;
                } else {
                    log.error("");
                }
                break;
            }

            /*
            case 780: 
            case 785: 
            case 789: 
            case 794: 
            case 799: 
            case 804: {
                if (epub.getfNumber() == 202) {
                    processRigaKanji1(false, table.select("tr").get(0));
                    processRigaRFrameCompo(false, table.select("tr").get(1));
                    return 2;
                } else {
                    log.error("");
                }
                break;
            }
            */
            case 880: {
                if (epub.getfNumber() == 202) {
                    processRigaKanji1(true, table.select("tr").first());
                    return 1;
                } else if (epub.getfNumber() == 203 && epub.getPass() == 1) {
                    processRigaRFrameCompo(false, table.select("tr").get(0));
                    return 2;
                } else {
                    log.error("");
                }
                break;
            }

            case 1137: {
                if (epub.getfNumber() == 300) {
                    processRigaKanji1(true, table.select("tr").first());
                    adHocKanji2(table, false,
                            table.select("tr").get(1).select("td").get(0).text(), // kanji 
                            null/*sgnPrim*/,
                            table.select("tr").get(1).select("td").get(2).text() /*reading*/, null, null);
                    return 1;
                } else if (epub.getfNumber() == 301 && epub.getPass() == 1) {
                    adHocKanji2(table, false,
                            null, // kanji 
                            null, // signal primitive
                            null, // reading
                            table.select("tr").get(0).select("td").get(0).text(), // link
                            table.select("tr").get(0).select("td").get(1).text() // R1 frame
                    );
                    processRigaRFrameCompo(false, table.select("tr").get(1));
                    return 2;
                } else {
                    log.error("");
                }
                break;
            }

            case 1664: {
                if (epub.getfNumber() == 305) {

                    // ha due composti, su stessa riga, usa <p> diversi per dare graficamente illusione 2 righe
                    // preservo questa finzione anche se forse sarebbe meglio spezzare su due vere righe/sub-entries
                    processRigaKanji1(false, table.select("tr").get(0));
                    processRigaRFrameCompo(false, table.select("tr").get(1));
                    return 2;
                } else if (epub.getfNumber() == 304) {
                    log.debug("");
                } else {
                    log.error("");
                }
                break;
            }

            case 1730: {
                if (epub.getfNumber() == 400) {

                    setKanji(table.select("tr").get(0).select("td").get(0).text());
                    setReadings(table.select("tr").get(0).select("td").get(2).text());
                    setComment("RTK1 frame nr manca nella versione elettronica, prenderlo da cartaceo");

                    processRigaRFrameCompo(false, table.select("tr").get(1));
                    return 2;
                } else if (epub.getfNumber() == 304) {
                    log.debug("");
                } else {
                    log.error("");
                }
                break;
            }

            case 1791: {
                if (epub.getfNumber() == 400) {
                    processRigaKanji1(false, table.select("tr").get(0));
                    return 1;
                } else if (epub.getfNumber() == 401) {
                    processRigaRFrameCompo(false, table.select("tr").get(0));
                    return 2;
                } else {
                    log.error("");
                }
                break;
            }
            case 1992: {
                if (epub.getfNumber() == 500) {
                    processRigaKanji1(false, table.select("tr").get(1));
                    return 1;
                } else if (epub.getfNumber() == 501) {
                    processRigaRFrameCompo(false, table.select("tr").get(0));
                    return 2;
                } else {
                    log.error("");
                }
                break;
            }

            /*
            case 2184: {
                if (epub.getfNumber() == 601) {
                    processRigaKanji1(false, table.select("tr").get(0));
                    processRigaRFrameCompo(false, table.select("tr").get(1));
                    return 2;
                } else {
                    log.error("");
                }
                break;
            } 
            */

            case 2211: {
                if (epub.getfNumber() == 601) {
                    processRigaKanji1(false, table.select("tr").get(0));
                    processRigaRFrameCompo(false, table.select("tr").get(1));
                    return 2;
                } else {
                    log.error("");
                }
                break;
            }

            case 2326: {
                if (epub.getfNumber() == 602) {
                    processRigaKanji1(false, table.select("tr").get(0));
                    setComment("tabella corrotta, sitemala a mano");
                    log.error("frame 2327 corrotto, inseriscilo a mano, forzo previou RFrame");
                    return 1;
                } else if (epub.getfNumber() == 603) {
                    epub.setPreviousRTK2Frame(2326);
                    setRTK2Frame(2327);
                    return 2;
                } else {
                    log.error("");
                }
                break;
            }

            default: {
            }
        }

        /*
        
            
            
            
            
            else if (epub.getCurTableNr() == 740 && epub.getfNumber() == 202) {
                if (epub.getPass() == 0) {
                    // --- riga kanji ---
                    setKanji(rows.get(0).select("td").get(1).text());
                    setReadings(rows.get(0).select("td").get(3).text());
                    setRTK1Frame(rows.get(0).select("td").get(5).text());
                    // --- riga RFrame ----
                    // impostata correttamente
                } else {
                    log.error("should not be completing");
                    System.exit(1);
                }
            } else if (epub.getCurTableNr() == 256) {
                if (epub.getfNumber() == 602) {
                    assert (epub.getPass() == 0);
                    // --- riga kanji ---
                    setReadings2(rows.get(1).select("td").get(2).text());
                    setKanji2(rows.get(1).select("td").get(0).text());
                    // --- riga RFrame ----
                    // impostata correttamente
                } else if (epub.getfNumber() == 603) {
                    assert (epub.getPass() == 0);
                    setRTK1Frame2(rows.get(0).select("td").get(0).text());
                    setLink2("");
                    setComment("");
                }
            } else {
                log.error(epub.getfNumber() + " " + epub.getCurTableNr());
                ret = false;
            }


        */

        return 0;
    }

    /**
     * For irreegular tables you specify the selector for the values
     * null selectors will be ignore
     * limited info usually found in kanji row not to have too many parameters
     * similar fuctions for other rows
     */
    boolean adHocKanji1(Element table, boolean overwrite, String kanjiPar, String sgnPrimPar, String readingPar,
            String linkPar, String R1FrPar) {
        if ((kanjiPar != null)
                && (getKanji() == null || overwrite)) {
            setKanji(kanjiPar);
        }

        if ((sgnPrimPar != null)
                && (getSignPrim() == null || overwrite)) {
            setSignPrim(sgnPrimPar);
        }

        if ((readingPar != null)
                && (getReadings() == null || overwrite)) {
            setReadings(readingPar);
        }

        if ((linkPar != null)
                && (getLink() == null || overwrite)) {
            setLink(linkPar);
        }

        if ((R1FrPar != null)
                && (getRTK1Frame() == -1 || overwrite)) {
            setRTK1Frame(Integer.parseInt(R1FrPar));
        }

        return true;
    }

    /**
     * For irreegular tables you specify the selector for the values
     * null selectors will be ignore
     * limited info usually found in kanji row not to have too many parameters
     * similar fuctions for other rows
     */
    boolean adHocKanji2(Element table, boolean overwrite, String kSel, String sgnPrim, String reading,
            String lnkSel, String R1FrSel) {
        if ((kSel != null)
                && (getKanji2() == null || overwrite)) {
            setKanji2(kSel);
        }

        if ((sgnPrim != null)
                && (getSignPrim2() == null || overwrite)) {
            setSignPrim2(sgnPrim);
        }

        if ((reading != null)
                && (getReadings2() == null || overwrite)) {
            setReadings2(reading);
        }

        if ((lnkSel != null)
                && (getLink2() == null || overwrite)) {
            setLink2(lnkSel);
        }

        if ((R1FrSel != null)
                && (getRTK1Frame2() == -1 || overwrite)) {
            setRTK1Frame2(Integer.parseInt(R1FrSel));
        }

        return true;
    }

    /**
     * For irreegular tables you specify the selector for the values
     * null selectors will be ignore
     * limited info usually found frame+compo row not to have too many parameters
     * similar fuctions for other rows
     */
    boolean adHocFrameCompo(boolean overwrite, String R2FramePar, String compoPar, String compoReadPar, String compoMeanPar) {

        if ((R2FramePar != null)
                && (getRTK2Frame() == -1 || overwrite)) {
            setRTK2Frame(R2FramePar);
        }

        if ((compoPar != null)
                && (getCompKanj() == null || overwrite)) {
            setCompKanj(compoPar);
        }
        if ((compoReadPar != null)
                && (getCompReading() == null || overwrite)) {
            setCompReading(compoReadPar);
        }
        if ((compoMeanPar != null)
                && (getCompMean() == null || overwrite)) {
            setCompMean(compoMeanPar);
        }

        return true;
    }

    /**
     * Esamina kanji  ....  tabelle con info nelle celle
     * @return
     */
    public boolean processCellTable(Element td, int fileNr, int tableNr, int cellNr) {

        String kanjiSel = ".x4-kanji";
        if (tableNr >= 57)
            kanjiSel = ".x2-lesson-text-77445-10-override";

        String kanji = td.select(kanjiSel).get(0).text();
        String RTK2FrameStr = td.select(".x2-r-number").get(0).text();
        String RTK1Frame = td.select(".x4-cross-reference-102345-10-filtered").get(0).text();

        setKanji(kanji);

        if (RTK2FrameStr.lastIndexOf("*") == -1)
            setRTK2Frame(RTK2FrameStr);
        else {
            setRTK2Frame(RTK2FrameStr.split("\\*")[0]);
        }
        setRTK1Frame(Integer.parseInt(RTK1Frame));

        epub.addToNoCNReadEntries(this);
        log.debug("r-" + getRTK2Frame() + " previous " + epub.getPreviousRTK2Frame() + " ok");
        epub.setPreviousRTK2Frame(getRTK2Frame());
        // log.info("r-" + getRTK2Frame() + " no chinese read - added ok" + toString());

        if (epub.getPreviousRTK2Frame() == 734)
            log.debug("");

        return false;
    }

    static boolean manualTable(int fileNr, int tableNr) {
        boolean ret = false;
        ret = ret || ((fileNr == 102) && (tableNr == 185));
        ret = ret || ((fileNr == 103) && (tableNr == 191));
        ret = ret || ((fileNr == 103) && (tableNr == 204));
        if (ret)
            log.debug("");
        return ret;
    }

    static boolean splitTable(int fileNr, int tableNr) {
        boolean ret = false;
        ret = ret || ((fileNr == 102 || fileNr == 103) && (tableNr == 185));
        ret = ret || ((fileNr == 103 || fileNr == 104) && (tableNr == 317));
        ret = ret || ((fileNr == 104 || fileNr == 105) && (tableNr == 446));
        if (ret)
            log.debug("");
        return ret;
    }

    public int getRTK2Frame() {
        return this.RTK2Frame;
    }

    public void setRTK2Frame(String sPar) {
        String s = sPar.substring(2);
        setRTK2Frame(Integer.parseInt(s));
    }

    public int getRTK1Frame() {
        return this.RTK1Frame;
    }

    public void setTableID(String id) {
        this.tableID = id;
    }

    public String getTableID() {
        return this.tableID;
    }

    public String getKanji() {
        return this.kanji;
    }

    public void setKanji(String k) {
        this.kanji = k;
    }

    public void setKanji2(String k) {
        this.kanji2 = k;
    }

    public String getSignPrim2() {
        return signPrim2;
    }

    public void setSignPrim2(String signPrim2) {
        this.signPrim2 = signPrim2;
    }

    public String getReadings2() {
        return readings2;
    }

    public void setReadings2(String readings2) {
        this.readings2 = readings2;
    }

    public String getLink2() {
        return link2;
    }

    public void setLink2(String link2) {
        this.link2 = link2;
    }

    public int getRTK1Frame2() {
        return RTK1Frame2;
    }

    public void setRTK1Frame2(int rTK1Frame2Par) {
        this.RTK1Frame2 = rTK1Frame2Par;
    }

    public String getKanji2() {
        return kanji2;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getSignPrim() {
        return signPrim;
    }

    public void setSignPrim(String signPrim) {
        this.signPrim = signPrim;
    }

    public String getReadings() {
        return readings;
    }

    public void setReadings(String readings) {
        this.readings = readings;
    }

    void setFillManually(int RTK2FramePar) {
        this.fillManually = true;
        this.kanji = "#";
        setRTK2Frame(RTK2FramePar);
    }

    public String getCompKanj2() {
        return compKanj2;
    }

    public void setCompKanj2(String compKanj2) {
        this.compKanj2 = compKanj2;
    }

    public String getCompReading2() {
        return compReading2;
    }

    public void setCompReading2(String compReading2) {
        this.compReading2 = compReading2;
    }

    public String getCompMean2() {
        return compMean2;
    }

    public void setCompMean2(String compMean2) {
        this.compMean2 = compMean2;
    }

    public int getEntryType() {
        return entryType;
    }

    public void setEntryType(int entryType) {
        this.entryType = entryType;
    }


    boolean                                fillManually;
    EPUB_main                              epub;

    int                                    entryType;

    String                                 capitolo;
    String                                 tableID;
    Selectors                              selectors;

    // --- riga Kanji 1
    String                                 kanji;
    String                                 signPrim;
    String                                 readings;
    String                                 link;
    int                                    RTK1Frame;
    // --- riga Kanji 1
    String                                 kanji2;
    String                                 signPrim2;
    String                                 readings2;
    String                                 link2;
    int                                    RTK1Frame2;

    // --- riga RFrame Compo1
    int                                    RTK2Frame;
    String                                 compKanj;
    String                                 compReading;
    String                                 compMean;
    // compo2
    String                                 compKanj2;
    String                                 compReading2;
    String                                 compMean2;

    // --- da riga 3
    String                                 comment;

    private static org.apache.log4j.Logger log = Logger.getLogger(EntryMain.class);
}
