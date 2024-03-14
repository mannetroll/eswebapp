package com.mannetroll.web.controller;

import java.util.Hashtable;

/**
 * Singelton-klass för hantering av kontrollsummor
 * <p>
 * Klassen <code>Checksum</code> beräknar (och kontrollerar)
 * kontrollsiffror för personnummer, kundnummer och kolliid.
 * Externa produkters kolliid hanteras separat (egen metod).
 * </p>
 * @version $Revision: 3 $, $Date: 02-01-28 14:19 $, $Author: Stsc002 $
 */
public class Checksum
{
  /******************
   * Klassvariabler *
   *****************/

  // Typer av giltiga id
  public static final int PARCEL_ID                   = 1;
  public static final int CUSTOMER_ID                 = 2;
  public static final int SOCIAL_SECURITY_NUMBER      = 3;
  public static final int OCR_REFERENCE_NUMBER		  = 4;
  public static final int EXTPROD_PARCEL_ID           = 5;

  // Längder
  private static final int CUSTOMER_ID_LEN            = 10;
  private static final int OLD_DPD_PARCEL_ID_LEN      = 12;
  private static final int DPD_PARCEL_ID_LEN          = 15;
  private static final int EAN_PARCEL_ID_LEN          = 20;
  private static final int PARCEL_ID_LEN              = 13;
  private static final int SOCIAL_SECURITY_NUMBER_LEN = 10;
  private static final int TNT_PARCEL_ID_LEN          = 9;
  private static final int FXAWB_PARCEL_ID_LEN        = 11;
  private static final int FXETN_PARCEL_ID_LEN		  = 12;  // Obs! Samma längd som old_DPD

  // Tabell med olika id
  private static Hashtable<String, Integer> m_htType = null;

  // Referens till den enda instansen av klassen
  private static Checksum m_cmInstance = null;

  /********************
   * Instansvariabler *
   *******************/

  /****************
   * Klassmetoder *
   ***************/

  /**
   * Instantierar (och initierar) den enda instansen av klassen.
   *
   * @return  Flagga som anger om klassen kunde instansieras.
   * @version 1.0
   */
  public static synchronized boolean init ()
  {
	boolean isInstantiated = false;

	if (m_cmInstance != null)
	{
	  // Klassen är redan instantierad
	  isInstantiated = true;
	}
	else
	{
	  try
	  {
	// Skapa den enda instansen av Checksum
	m_cmInstance = new Checksum();

	// Skapa tabell med giltiga typer av id
	m_htType = new Hashtable<String, Integer>();

	m_htType.put(new String("customer_id"), Integer.valueOf(CUSTOMER_ID));
	m_htType.put(new String("kundnummer"), Integer.valueOf(CUSTOMER_ID));
	m_htType.put(new String("kundnr"), Integer.valueOf(CUSTOMER_ID));

	m_htType.put(new String("social_security_number"), Integer.valueOf(SOCIAL_SECURITY_NUMBER));
	m_htType.put(new String("personnummer"), Integer.valueOf(SOCIAL_SECURITY_NUMBER));
	m_htType.put(new String("personnr"), Integer.valueOf(SOCIAL_SECURITY_NUMBER));
	m_htType.put(new String("persnr"), Integer.valueOf(SOCIAL_SECURITY_NUMBER));

	m_htType.put(new String("parcel_id"), Integer.valueOf(PARCEL_ID));
	m_htType.put(new String("kolliid"), Integer.valueOf(PARCEL_ID));

	m_htType.put(new String("ocrReferensnummer"), Integer.valueOf(OCR_REFERENCE_NUMBER));
	m_htType.put(new String("ocr_reference_number"), Integer.valueOf(OCR_REFERENCE_NUMBER));

	m_htType.put(new String("extprod_kolliId"), Integer.valueOf(EXTPROD_PARCEL_ID));

	// Ange att klassen instantierades (och initierades) utan fel
	isInstantiated = true;
	  }
	  catch (Exception e)
	  {
	// Nollställ referenser
	m_cmInstance = null;
	m_htType = null;

	// Kunde inte skapa en instans av klassen
	isInstantiated = false;
	  }
	}

	return isInstantiated;
  }

  /**
   * Hämta den enda instansen av klassen.
   *
   * @return  Den enda instansen av klassen Checksum.
   * @version 1.0
   */
  public static Checksum instance()
  {
	return getInstance();
  }

  /**
   * Hämta den enda instansen av klassen.
   *
   * @return  Den enda instansen av klassen Checksum.
   * @version 1.0
   */
  public static Checksum getInstance()
  {
	if (m_cmInstance == null)
	{
	  synchronized (Checksum.class)
	  {
	if (!Checksum.init())
	{
	  return null;
	}
	  }
	}

	return (Checksum)m_cmInstance;
  }

  /******************
   * Instansmetoder *
   *****************/

  /**************
   * Algoritmer *
   *************/

  /**
   * Beräkna kontrollsiffra enligt vägd modulus 10: vikter {2, 1}
   *
   * @param     p_strId Id
   * @return    Kontrollsiffra
   * @exception NullPointerException om inparameter saknas.
   * @version   1.0
   */
  private int getModulo10Checksum (String p_strId)
  {
	int iChecksum;           // Kontrollsiffra
	int iIndex = 0;          // Index för vikt
	int iProduct;
	int iSum = 0;
	int iWeights[] = {2, 1}; // Vikter

	for (int i = p_strId.length() - 1; i >= 0; i--)
	{
	  // Beräkna produkten...
	  int iNumber = p_strId.charAt(i) - '0';
	  iProduct = iNumber * iWeights[iIndex];

	  // ...och ta fram delsumman (10 = 1+0, 11 = 1+1, osv.)
	  iSum = iSum + (iProduct/10) + (iProduct % 10);

	  // Ta fram nästa vikt
	  iIndex++;
	  iIndex = iIndex % iWeights.length;
	}

	iChecksum = iSum % 10;

	if (iChecksum != 0)
	{
	  iChecksum = 10 - iChecksum;
	}

	// Returnera kontrollsiffran
	return iChecksum;
  }

  /**
   * Beräkna kontrollsiffra enligt vägd modulus 11:
   * vikter {7, 9, 5, 3, 2, 4, 6, 8}
   *
   * @param     p_strId Id
   * @return    Kontrollsiffra
   * @exception NullPointerException om inparameter saknas.
   * @version   1.0
   */
  private int getModulo11Checksum (String p_strId)
  {
	int iChecksum;                             // Kontrollsiffra
	int iIndex = 0;                            // Index för vikt
	int iSum = 0;
	int iWeights[] = {7, 9, 5, 3, 2, 4, 6, 8}; // Vikter

	for (int i = p_strId.length() - 1; i >= 0; i--)
	{
	  // Beräkna delsumman
	  int iNumber = p_strId.charAt(i) - '0';
	  iSum = iSum + (iNumber * iWeights[iIndex]);

	  // Ta fram nästa vikt
	  iIndex++;
	  iIndex = iIndex % iWeights.length;
	}

	iChecksum = iSum % 11;

	if (iChecksum == 0)
	{
	  iChecksum = 5;
	}
	else if (iChecksum == 1)
	{
	  iChecksum = 0;
	}
	else
	{
	  iChecksum = 11 - iChecksum;
	}

	// Returnera kontrollsiffra
	return iChecksum;
  }

  /**
   * Beräkna kontrollsiffra enligt vägd modulus 11:
   * vikter { 3, 1, 7 }
   *
   * @param     p_strId Id
   * @return    Kontrollsiffra
   * @exception NullPointerException om inparameter saknas.
   * @version   1.0
   */
  private int getModulo11ChecksumWeight3 (String p_strId)
  {
      int[] modSerie = { 3, 1, 7 };
      int modSerieIndex = 0;
      int sum = 0;
      int result = 0;

      for (int i = 0; i < p_strId.length(); i++)
      {
          if (modSerieIndex > 2)
              modSerieIndex = 0;

          int iNumber = p_strId.charAt(i) - '0';
          sum += iNumber * modSerie[modSerieIndex];
          modSerieIndex++;
      }

      result = sum % 11;

      if (result == 10)
          result = 0;

  	// Returnera kontrollsiffra
      return result;
  }

  /**
   * Beräkna kontrollsiffra för streckkod (EAN)
   *
   * @param     p_strEAN EAN-nummer
   * @exception NullPointerException om inparameter saknas.
   * @version   1.0
   */
  private int getEANChecksum (String p_strEAN)
  {
	int iChecksum = 0;
	int iIndex = 0;
	int iSum = 0;
	int iWeights[] = {3, 1};

	for (int i = 0; i < p_strEAN.length(); i++)
	{
	  // Beräkna delsumma
	  int iNumber = p_strEAN.charAt(i) - '0';
	  iSum = iSum + iNumber * iWeights[iIndex];

	  // Ta fram nästa vikt
	  iIndex++;
	  iIndex = iIndex % iWeights.length;
	}

	iChecksum = iSum % 10;

	if (iChecksum != 0)
	{
	  iChecksum = 10 - iChecksum;
	}

	// Returnera kontrollsiffra
	return iChecksum;
  }

  /**
   * Beräkna kontrollsiffra enligt modulus 7 (utan nyckel/vikt)
   *
   * @param     p_strId Id
   * @return    Kontrollsiffra
   * @exception NullPointerException om inparameter saknas.
   * @version   1.0
   */
  private int getModulo7Checksum (String p_strId)
  {

    long result = 0;
    result = Long.parseLong(p_strId);

	// Returnera kontrollsiffra
    return (int)result % 7;
  }

  /**
   * Beräkna kontrollsiffra för DPD-kollin
   *
   * @param     p_strKolliId DPD-kolliid
   * @exception NullPointerException om inparameter saknas.
   * @version   1.0
   */
  public int getOldDPDChecksum (String p_strKolliId)
  {
	int iChecksum = 0;
	int iIndex = 0;
	int iProduct;
	int iSum = 0;
	int iWeights[] = {3, 1}; // Observera att vikterna är annorlunda
							 // jämfört med getModulo10Checksum().

	for (int i = 0; i < p_strKolliId.length(); i++)
	{
	  // Beräkna produkten...
	  int iNumber = p_strKolliId.charAt(i) - '0';
	  iProduct = iNumber * iWeights[iIndex];

	  // ...och ta fram delsumman
	  iSum = iSum + iProduct;

	  // Ta fram nästa vikt
	  iIndex++;
	  iIndex = iIndex % iWeights.length;
	}

	iChecksum = iSum % 10;

	if (iChecksum != 0)
	{
	  iChecksum = 10 - iChecksum;
	}

	// Returnera kontrollsiffra
	return iChecksum;
  }

	/**
	 *  Calculate the ISO/IEC 7064 check digit
	 *  with the defined modulo value.
	 *  Input:  Pointer to the data buffer
	 *  Return: ISO check digit value
	 */
	public char getDPDChecksum(String p_strId)
	{
	     int ISO7064_MODULO = 36;

	    /* tabell för att konvertera ascii-tecken till ISO/IEC 7064 värde */
		int ascii2isoval[] =
		{
			 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
			 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
			 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
			 0,  1,  2 , 3,  4,  5,  6,  7,  8,  9,  0,  0,  0,  0,  0,  0,
			 0, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
			25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35,  0,  0,  0,  0,  0,
			 0, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
			25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35,  0,  0,  0,  0,  0
		};

	    /* tabell för att konvertera ISO/IEC 7064 värde till ascii-tecken */
		char isoval2ascii[] =
		{
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
			'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
			'U', 'V', 'W', 'X', 'Y', 'Z', '*'
		};

		int modulo;
		int checkDigit;
		int i;

		for (checkDigit = modulo = ISO7064_MODULO, i = 0; i < p_strId.length(); i++)
		{
			checkDigit += ascii2isoval[((int)p_strId.charAt(i))];

			if (checkDigit > modulo)
				checkDigit -= modulo;

			checkDigit *= 2;

			if (checkDigit > modulo)
				checkDigit -= modulo + 1;

		}

		checkDigit = modulo + 1 - checkDigit;

		if (checkDigit == modulo)
			checkDigit = 0;

		return (isoval2ascii[checkDigit]);
	}

	/******************
   * Övriga metoder *
   *****************/

  /**
   * Kontrollera om samtliga tecken i en sträng är siffror.
   * <p>
   * Metoden <code>isNumeric</code> kontrollerar om samtliga tecken
   * i en sträng är siffror.
   * </p>
   * @param     p_strArg Sträng som skall kontrolleras
   * @return    Flagga som anger om strängen endast innehåller siffror.
   * @exception IllegalArgumentException om null skickas som
   * argument till metoden.
   * @version 1.0
   */
  private boolean isNumeric (String p_strArg)
  {
	if (p_strArg == null)
	{
	  throw new IllegalArgumentException("Felaktigt inargument");
	}

	int iLenght = p_strArg.length();
	for (int i = 0; i < iLenght; i++)
	{
	  if (!Character.isDigit(p_strArg.charAt(i)))
	  {
	return false;
	  }
	}

	return true;
  }

  /**
   * Kontrollera id
   *
   * @param   p_strId   Id
   * @param   p_strType Typ av id
   * @return  Flagga som anger om det är ett giltigt id.
   * @version 1.0
   */
  public boolean check(String p_strId, String p_strType)
  {
	return isValid(p_strId, p_strType);
  }

  /**
   * Kontrollera id
   *
   * @param   p_strId Id
   * @param   p_iType Typ av id
   * @return  Flagga som anger om det är ett giltigt id.
   * @version 1.0
   */
  public boolean check(String p_strId, int p_iType)
  {
	return isValid(p_strId, p_iType);
  }

  /**
   * Kontrollera checksumma för id
   *
   * @param     p_strId   Id
   * @param     p_strType Typ av id
   * @return    Flagga som anger om det är ett giltigt id.
   * @exception IllegalArgumentException om det id som anges är null.
   * @version   1.0
   */
  public boolean isValid(String p_strId, String p_strType)
  {
	if (p_strId == null)
	{
	  throw new IllegalArgumentException("Felaktigt id: null");
	}

	String strKey = p_strType.toLowerCase(); // Typ av id
	boolean bValid = false;                  // Flagga som anger om det är ett giltigt id
	int iType = -1;                          // Typ av id

	if (m_htType.containsKey(strKey))
	{
	  iType = ((Integer)m_htType.get(strKey)).intValue();
	}

	// Kontrollera om det är ett giltigt id
	bValid = isValid(p_strId, iType);

	return bValid;
  }

  /**
   * Kontrollera checksumma för id
   *
   * @param     p_strId Id
   * @param     p_iType Typ av id
   * @return    Flagga som anger om det är ett giltigt id.
   * @exception IllegalArgumentException om en det id som skickas till metoden
   * är null eller om en felaktig typ anges.
   * @version   1.0
   */
  public boolean isValid(String p_strId, int p_iType)
  {
	if (p_strId == null)
	{
	  throw new IllegalArgumentException("Felaktigt id: null");
	}

	String strId = p_strId.trim(); // Id
	boolean bValid = false;        // Flagga för giltigt id

	// Kontrollera att rätt typ har angivits för id:t
	if (!m_htType.containsValue(Integer.valueOf(p_iType)))
	{
	  throw new IllegalArgumentException("Felaktig typ: " +
					 p_iType);
	}

	/*
	 * Anropa rätt metod för kontroll av id
	 *
	 * Lägg till nya rader här vartefter nya
	 * kontrollmetoder tillkommer!
	 */
	switch (p_iType)
	{
	case CUSTOMER_ID:
	  bValid = checkCustomerId(strId);
	  break;
	case PARCEL_ID:
	  bValid = checkParcelId(strId);
	  break;
	case SOCIAL_SECURITY_NUMBER:
	  bValid = checkSocialSecurityNumber(strId);
	  break;
	case OCR_REFERENCE_NUMBER:
	  bValid = checkOcrReferenceNumber(strId);
	  break;
	case EXTPROD_PARCEL_ID:
		  bValid = checkParcelIdForExternalProducts(strId);
		  break;
	}

	return bValid;
  }

  /**
   * Kontrollera kundnummer
   *
   * @param   p_strId Kundnummer
   * @return  Flagga som anger om kundnumret är korrekt
   * @version 1.0
   */
  private boolean checkCustomerId (String p_strId)
  {
	boolean isValid = false; // Flagga som anger om kundnumret är korrekt
	int iChecksum;           // Kontrollsiffra

	// Kontrollera längden på kundnumret
	// Måste vara 10 tecken - alltid!
	if (p_strId.length() < CUSTOMER_ID_LEN)
	{
	  return false;
	}

	// Beräkna och kontrollera kontrollsiffran
	iChecksum = p_strId.charAt(CUSTOMER_ID_LEN - 1) - '0';

	if (iChecksum == getModulo10Checksum(p_strId.substring(0, CUSTOMER_ID_LEN - 1)))
	{
	  isValid = true;
	}

	return isValid;
  }

  /**
   * Kontrollera kolliid
   *
   * @param   p_strId Kolliid
   * @return  Flagga som anger om det är ett korrekt kolliid
   * @version 1.0
   */
    private boolean checkParcelId (String p_strId)
  {
	int iStart;              // Startposition i kolliid:t
	int iChecksum;           // Kontrollsiffra
	String strKolliid;       // Kolliid
	boolean isValid = false; // Flagga som anger om kolliid:t är korrekt

	// Ta fram längden på kolliid:t
	int iLength = p_strId.length();

	// Hantera inte UPU
	if (!p_strId.startsWith("JJ"))
	{
	  switch (iLength)
	  {
	  case OLD_DPD_PARCEL_ID_LEN:
	{
	  // Ta fram kontrollsiffra
	  iChecksum = p_strId.charAt(OLD_DPD_PARCEL_ID_LEN - 1) - '0';

	  // ...och kolliid:t
	  strKolliid = p_strId.substring(0, OLD_DPD_PARCEL_ID_LEN - 1);

	  // Beräkna och kontrollera kontrollsiffran
	  if (isNumeric(strKolliid))
	  {
		if (iChecksum == getOldDPDChecksum(strKolliid))
		{
		  isValid = true;
		}
	  }
	}
	break;

	  case PARCEL_ID_LEN:
	{
	  // Ta fram kontrollsiffra
	  iChecksum = p_strId.charAt(PARCEL_ID_LEN - 3) - '0';

	  // Börjar kolliid med ett tecken?
	  // Ange rätt startposition
	  if (Character.isDigit(p_strId.charAt(1)))
	  {
		iStart = 0;
	  }
	  else
	  {
		iStart = 2;
	  }

	  // Beräkna och kontrollera kontrollsiffran
	  strKolliid = p_strId.substring(iStart, PARCEL_ID_LEN - 3);
	  if (isNumeric(strKolliid))
	  {
		if (iChecksum == getModulo11Checksum(strKolliid))
		{
		  isValid = true;
		}
	  }
	}
	break;

	  case EAN_PARCEL_ID_LEN:
	{
	  iChecksum = p_strId.charAt(EAN_PARCEL_ID_LEN - 1) - '0';
	  strKolliid = p_strId.substring(0, EAN_PARCEL_ID_LEN - 1);

	  // Beräkna och kontrollera kontrollsiffran
	  if (isNumeric(strKolliid))
	  {
		if (iChecksum == getEANChecksum(strKolliid))
		{
		  isValid = true;
		}
	  }
	}
	break;

	  default:
	break;
	  }
	}
    if (iLength == DPD_PARCEL_ID_LEN)
	{
	  // Ta fram kontrolltecken
      char checkTecken = p_strId.charAt(DPD_PARCEL_ID_LEN - 1);

      // ...och kolliid:t
	  strKolliid = p_strId.substring(0, DPD_PARCEL_ID_LEN - 1);

	  // Beräkna och kontrollera kontrollsiffran
	  if (checkTecken == getDPDChecksum(strKolliid))
		{
		  isValid = true;
		}
	}

	return isValid;
  }

  /**
   * Kontrollera kolliid för externa produkter
   *
   * @param   p_strId Kolliid
   * @return  Flagga som anger om det är ett korrekt kolliid
   * @version 1.0
   */
  private boolean checkParcelIdForExternalProducts (String p_strId)
  {
	int iChecksum;           // Kontrollsiffra
	String strKolliid;       // Kolliid
	boolean isValid = false; // Flagga som anger om kolliid:t är korrekt

	// Ta fram längden på kolliid:t
	int iLength = p_strId.length();

	switch (iLength)
	  {
	  // FedEx ETN
	  case FXETN_PARCEL_ID_LEN:
	{
	  // Ta fram kontrollsiffra
	  iChecksum = p_strId.charAt(FXETN_PARCEL_ID_LEN - 1) - '0';

	  // ...och kolliid:t
	  strKolliid = p_strId.substring(0, FXETN_PARCEL_ID_LEN - 1);

	  // Beräkna och kontrollera kontrollsiffran
	  if (isNumeric(strKolliid))
	  {
		if (iChecksum == getModulo11ChecksumWeight3(strKolliid))
		{
		  isValid = true;
		}
	  }
	}
	break;

	  case TNT_PARCEL_ID_LEN:
	{
		  // Ta fram kontrollsiffra
		  iChecksum = p_strId.charAt(TNT_PARCEL_ID_LEN - 1) - '0';
		  // Beräkna och kontrollera kontrollsiffran
		  strKolliid = p_strId.substring(0, TNT_PARCEL_ID_LEN - 1);

		  if (isNumeric(strKolliid))  {
			if (iChecksum == getModulo11Checksum(strKolliid)) {
			  isValid = true;
			} else {
				if (iChecksum == getModulo7Checksum(strKolliid)) {
					  isValid = true;
				}
			}
		  }
	}
	break;

	  case FXAWB_PARCEL_ID_LEN:
		{
		  // Ta fram kontrollsiffra
		  iChecksum = p_strId.charAt(FXAWB_PARCEL_ID_LEN - 1) - '0';
		  // Beräkna och kontrollera kontrollsiffran
		  strKolliid = p_strId.substring(3, FXAWB_PARCEL_ID_LEN - 1);

		  if (iChecksum == getModulo7Checksum(strKolliid)) {
						  isValid = true;
		  }
		}

	break;

	  default:
	break;
	  }

	return isValid;
  }


  /**
   * Kontrollera personnummer
   * <p>
   * Metoden <code>checkSocialSecurityNumber</code> kontrollerar om ett
   * personnummer är korrekt angivet. Kontrollsiffran beräknas på ett
   * personnummer med utseendet YYMMDD-XXXX. Observera att metoden även
   * arbetar med personnummer på formatet YYYYMMDD-XXXX.
   * </p>
   *
   * @param   p_strId Personnummer
   * @return  Flagga som anger om personnumret är korrekt.
   * @version 1.0
   */
  private boolean checkSocialSecurityNumber (String p_strId)
  {
	String strId;            // Personnummer
	boolean isValid = false; // Flagga som anger om personnumret är giltigt
	int iChecksum;           // Kontrollsiffra
	int iStart = -1;         // Startposition i strängen för beräkning av personnummer

	// Sätt rätt startposition i strängen
	int iLength = p_strId.length();
	if (iLength == SOCIAL_SECURITY_NUMBER_LEN)
	{
	  iStart = 0;
	}
	else if (iLength == (SOCIAL_SECURITY_NUMBER_LEN + 2))
	{
	  iStart = 2;
	}

	// Beräkna (och kontrollera) kontrollsiffran
	if (iStart != -1)
	{
	  strId = p_strId.substring(iStart, iLength - 1);
	  iChecksum = p_strId.charAt(iLength - 1) - '0';

	  if (isNumeric(strId))
	  {
	if (iChecksum == getModulo10Checksum(strId))
	{
	  isValid = true;
	}
	  }
	}

	return isValid;
  }

  /**
   * Kontrollera ocrreferens
   * <p>
   * Metoden <code>checkOcrReferenceNumber</code> kontrollerar om en
   * ocrreferens är korrekt angivet.
   * </p>
   *
   * @param   p_strId ocrreferens
   * @return  Flagga som anger om referensen är korrekt.
   * @version 1.0
   */
  private boolean checkOcrReferenceNumber(String p_strId) {
	int iChecksum = p_strId.charAt(p_strId.length() - 1) - '0';
	String ocrReferens = p_strId.substring(0, p_strId.length() - 1);
	boolean isValid = false;

	// Beräkna och kontrollera kontrollsiffran
	if (isNumeric(ocrReferens) && iChecksum == getModulo10Checksum(ocrReferens)) {
		isValid = true;
	}
  	return isValid;
  }

  /*****************
   * Konstruktorer *
   ****************/

  /*
   * Default-konstruktor (privat)
   *
   * @version 1.0
   */
  private Checksum ()
  {
  }

}
