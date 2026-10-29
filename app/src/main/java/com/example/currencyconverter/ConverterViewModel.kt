package com.example.currencyconverter

import androidx.lifecycle.ViewModel

/** Garde l'état de l'écran pendant les rotations. */
class ConverterViewModel : ViewModel() {
    var montant: String = "0"
    var resultat: String = ""
    var positionSource: Int = 0   // USD par défaut
    var positionCible: Int = 1    // EUR par défaut
}