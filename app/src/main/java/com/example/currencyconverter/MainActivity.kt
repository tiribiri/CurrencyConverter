package com.example.currencyconverter

import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.currencyconverter.databinding.ActivityMainBinding
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Le ViewModel survit aux rotations : il garde montant, résultat et devises
    private val viewModel: ConverterViewModel by viewModels()

    // Taux de change fixes pour 1 EUR (l'euro est la devise pivot)
    private val taux = mapOf(
        "EUR" to 1.0, "USD" to 1.17, "TND" to 3.40,
        "GBP" to 0.87, "JPY" to 172.50, "CAD" to 1.62
    )

    private val longueurMaxMontant = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        souslignerAffichages()
        remplirSpinners()
        restaurerEtat()               // avant les listeners, pour ne pas écraser l'état
        configurerListenersSpinners()
        configurerClavier()
        configurerBoutons()
    }

    private fun souslignerAffichages() {
        binding.tvMontant.paintFlags = binding.tvMontant.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.tvResultat.paintFlags = binding.tvResultat.paintFlags or Paint.UNDERLINE_TEXT_FLAG
    }

    private fun remplirSpinners() {
        val adapter = ArrayAdapter.createFromResource(
            this, R.array.devises, R.layout.spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spDeviseSource.adapter = adapter
        binding.spDeviseCible.adapter = adapter
    }

    /** Réaffiche l'état du ViewModel (au premier lancement : 0, USD -> EUR). */
    private fun restaurerEtat() {
        binding.tvMontant.text = viewModel.montant
        binding.tvResultat.text = viewModel.resultat
        binding.spDeviseSource.setSelection(viewModel.positionSource)
        binding.spDeviseCible.setSelection(viewModel.positionCible)
    }

    /** Un seul listener pour les deux Spinner : mémorise la position puis reconvertit. */
    private fun configurerListenersSpinners() {
        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                if (parent === binding.spDeviseSource) {
                    viewModel.positionSource = position
                } else {
                    viewModel.positionCible = position
                }
                convertirAutomatiquement()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        binding.spDeviseSource.onItemSelectedListener = listener
        binding.spDeviseCible.onItemSelectedListener = listener
    }

    private fun configurerClavier() {
        val touches = mapOf(
            binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2",
            binding.btn3 to "3", binding.btn4 to "4", binding.btn5 to "5",
            binding.btn6 to "6", binding.btn7 to "7", binding.btn8 to "8",
            binding.btn9 to "9", binding.btnPoint to "."
        )
        for ((bouton, caractere) in touches) {
            bouton.setOnClickListener { ajouterCaractere(caractere) }
        }

        // Touche C : remet le montant à 0 et vide le résultat
        binding.btnC.setOnClickListener {
            viewModel.montant = getString(R.string.montant_initial)
            viewModel.resultat = ""
            binding.tvMontant.text = viewModel.montant
            binding.tvResultat.text = viewModel.resultat
        }
    }

    private fun configurerBoutons() {
        // Bouton Convertir : seul endroit où le Toast d'erreur s'affiche
        binding.btnConvertir.setOnClickListener {
            val montant = viewModel.montant.toDoubleOrNull() ?: 0.0
            if (montant == 0.0) {
                Toast.makeText(this, R.string.erreur_montant, Toast.LENGTH_SHORT).show()
            } else {
                convertirAutomatiquement()
            }
        }

        // Inversion : on échange les positions ; les listeners relancent la conversion
        binding.btnInverser.setOnClickListener {
            val positionSource = binding.spDeviseSource.selectedItemPosition
            val positionCible = binding.spDeviseCible.selectedItemPosition
            binding.spDeviseSource.setSelection(positionCible)
            binding.spDeviseCible.setSelection(positionSource)
        }
    }

    /** Ajoute un chiffre ou un point (un seul point, 10 caractères max, "0" initial remplacé). */
    private fun ajouterCaractere(c: String) {
        val montantActuel = viewModel.montant

        if (c == "." && montantActuel.contains(".")) return
        if (montantActuel.length >= longueurMaxMontant) return

        viewModel.montant =
            if (montantActuel == "0" && c != ".") c else montantActuel + c
        binding.tvMontant.text = viewModel.montant

        convertirAutomatiquement()
    }

    /**
     * Conversion silencieuse (sans Toast), appelée à chaque saisie ou changement de devise.
     * Si le montant vaut 0, le résultat est simplement vidé.
     */
    private fun convertirAutomatiquement() {
        val montant = viewModel.montant.toDoubleOrNull() ?: 0.0

        if (montant == 0.0) {
            viewModel.resultat = ""
        } else {
            val source = binding.spDeviseSource.selectedItem.toString()
            val cible = binding.spDeviseCible.selectedItem.toString()
            // Locale.US : le point reste le séparateur décimal
            viewModel.resultat =
                String.format(Locale.US, "%.2f", convertir(montant, source, cible))
        }
        binding.tvResultat.text = viewModel.resultat
    }

    /** Conversion via l'euro : montant / taux(source) * taux(cible). */
    private fun convertir(montant: Double, source: String, cible: String): Double {
        val montantEnEuro = montant / taux.getValue(source)
        return montantEnEuro * taux.getValue(cible)
    }
}