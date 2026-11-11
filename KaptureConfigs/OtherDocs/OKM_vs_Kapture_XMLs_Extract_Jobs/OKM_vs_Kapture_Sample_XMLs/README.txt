OKM vs Kapture sample XMLs
==========================

One sample pair per channel for each export tool. Files are unchanged copies of the export output.
Images, PDFs and other attachments are not included.

Kapture samples come from RMI Export job 308 and MNAO Data Export job 33 (MC Dev, 2026-11-02).
OKM samples come from RMI Export job 830 and MNAO Data Export jobs 281 and 261.
Document ids differ between OKM and Kapture, so pairs were matched by title.
Header node names also differ; see the XML structure document.

RMI_Export / SERVICE_MANUALS
  OKM : SM3807252.xml
  Kapture: SM251673.xml
  Title  : BLOWER MOTOR INSPECTION
  Note   : Same title. CX-5 KM, en_UK, Workshop Manual.

RMI_Export / WIRING_DIAGRAMS
  OKM : WD811778.xml
  Kapture: WD26133.xml
  Title  : HORN [0922-2]
  Note   : Same title. CX-5 KM, en_UK.

MNAO_Data_Export / SERVICE_MANUALS
  OKM : SM3277780.xml
  Kapture: SM148463.xml
  Title  : ANTICORROSION, SOUND INSULATION, AND VIBRATION INSULATION
  Note   : Same title and Bodyshop Manual type. OKM sample is CX-90, Kapture sample is CX-5, en_US.

MNAO_Data_Export / WIRING_DIAGRAMS
  OKM : WD571182.xml
  Kapture: WD27656.xml
  Title  : ELECTRIC POWER STEERING (EPS)_U300316
  Note   : Same title. OKM sample is CX-90, Kapture sample is CX-5, en_US.

MNAO_Data_Export / SERVICE_INFORMATION
  OKM : SI123724.xml
  Kapture: SI107921.xml
  Title  : 09-039/16 - 2016 CX-5 - REAR CONSOLE LID DOES NOT LOCK  (OKM: 09-019/25 - EXTERIOR LIGHT WARNING WITH DTC B10A3:86)
  Note   : DIFFERENT documents: no common document exists in the available extracts. Both are Technical Service Bulletins, en_US; OKM CX-90, Kapture CX-5. Compare structure, not text.

Bulk Extract samples
--------------------

Kapture samples come from extract job 731 (MC Dev, 2026-11-02). OKM samples come from the extract of 2026-10-20.
Document ids differ between OKM and Kapture. Files are unchanged copies of the extract output.

Bulk_Extract / SERVICE_INFORMATION
  OKM : SI125208.xml
  Kapture: SI126491.xml
  Title  : 01-014/15 - MULTI-MODEL - ENGINE OIL SEEPAGE FROM ENGINE OIL PRESSURE SWITCH  (OKM: 16-004/26 - UPDATED SOFTWARE RELEASE FOR COCKPIT DOMAIN CONTROLLER (CDC) AND DATA COMMUNICATION MODULE (DCM))
  Note   : DIFFERENT documents: no common document exists in the available extracts. Both are Technical Service Bulletins, en_US, CX-5. Compare structure, not text.

Bulk_Extract / SERVICE_MANUALS
  OKM : SM3864229.xml
  Kapture: SM264928.xml
  Title  : CUP HOLDER REMOVAL/INSTALLATION
  Note   : Same title. Workshop Manual, en_US, CX-5.

Bulk_Extract / WIRING_DIAGRAMS
  OKM : WD569715.xml
  Kapture: WD27656.xml
  Title  : ELECTRIC POWER STEERING (EPS)_U300316  (OKM: DATA LINK CONNECTOR [00D-1])
  Note   : DIFFERENT documents: no common document exists in the available extracts. en_US; OKM MAZDA3, Kapture CX-5. Compare structure, not text.

Translation Update samples
--------------------------

Kapture samples come from Translation Update export jobs 404 and 403 (MC Dev, 2026-11-02). The OKM samples come from MDM exports in OKM.
Document ids differ between OKM and Kapture. Files are unchanged copies of the export output.
OKM and Kapture have the same node format for both channels: XML declaration, channel name as root node, values in CDATA, and a node the document does not have written as empty CDATA.

Translation_Update / SERVICE_INFORMATION
  OKM    : SI125458.xml
  Kapture: SI125398.xml
  Title  : AB018A: Mazda6, Mazda3, CX-7, MX-5 - Tire Pressure Monitoring System (TPMS) - Request for Mazda Service Program  (OKM: E035/26: CX-6e (HDC) - CX-6e Hood Closing Characteristics)
  Note   : DIFFERENT documents: no common document exists in the available extracts. OKM nl_BE, Kapture de_DE. Same root node and same 11 value nodes in the same order. Compare structure, not text.

Translation_Update / SERVICE_MANUALS
  OKM    : SM3867845.xml
  Kapture: SM208074.xml
  Title  : DTC P0A94:00 [SKYACTIV-G 2.0, SKYACTIV-G 2.5]  (OKM: SOPHISTICATED AIRBAG SENSOR CONTROL MODULE (SAS_CM) REPLACEMENT)
  Note   : DIFFERENT documents: no common document exists in the available extracts. OKM en_UK, Kapture cs_CZ. Same root node and same 4 value nodes in the same order. Compare structure, not text.
