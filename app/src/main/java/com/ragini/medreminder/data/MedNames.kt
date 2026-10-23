package com.ragini.medreminder.data

/**
 * Common medicine names (generic names and widely used Indian brand names), offered as
 * suggestions while typing so names are spelled correctly. Anything can still be typed freely.
 */
object MedNames {
    val common: List<String> = listOf(
        // Blood pressure & heart
        "Amlodipine", "Telmisartan", "Losartan", "Olmesartan", "Ramipril", "Enalapril", "Metoprolol", "Atenolol",
        "Bisoprolol", "Carvedilol", "Nebivolol", "Cilnidipine", "Hydrochlorothiazide", "Chlorthalidone", "Furosemide",
        "Torsemide", "Spironolactone", "Prazosin", "Clonidine", "Isosorbide Mononitrate", "Nitroglycerin", "Ivabradine",
        "Digoxin", "Amiodarone", "Ranolazine", "Telma", "Telma H", "Amlong", "Stamlo", "Losar", "Met XL", "Cardace",
        // Cholesterol & blood thinners
        "Atorvastatin", "Rosuvastatin", "Simvastatin", "Fenofibrate", "Ezetimibe", "Aspirin", "Ecosprin", "Ecosprin AV",
        "Clopidogrel", "Prasugrel", "Ticagrelor", "Warfarin", "Acenocoumarol", "Apixaban", "Rivaroxaban", "Dabigatran",
        "Atorva", "Rozavel", "Clopilet",
        // Diabetes
        "Metformin", "Glimepiride", "Gliclazide", "Glipizide", "Sitagliptin", "Vildagliptin", "Teneligliptin",
        "Linagliptin", "Dapagliflozin", "Empagliflozin", "Canagliflozin", "Pioglitazone", "Voglibose", "Acarbose",
        "Insulin Glargine", "Insulin Aspart", "Insulin Lispro", "Human Insulin", "Glycomet", "Glycomet GP", "Janumet",
        "Galvus Met", "Jardiance", "Forxiga", "Amaryl", "Istamet",
        // Thyroid
        "Levothyroxine", "Thyroxine", "Thyronorm", "Eltroxin", "Carbimazole", "Methimazole",
        // Pain, fever, joints
        "Paracetamol", "Dolo 650", "Crocin", "Calpol", "Ibuprofen", "Diclofenac", "Aceclofenac", "Naproxen",
        "Etoricoxib", "Tramadol", "Combiflam", "Zerodol", "Hifenac", "Colchicine", "Allopurinol", "Febuxostat",
        "Glucosamine", "Hydroxychloroquine", "Methotrexate", "Prednisolone", "Deflazacort",
        // Stomach
        "Pantoprazole", "Pan 40", "Pan D", "Omeprazole", "Rabeprazole", "Esomeprazole", "Famotidine", "Ranitidine",
        "Domperidone", "Ondansetron", "Itopride", "Sucralfate", "Lactulose", "Isabgol", "Bisacodyl", "Loperamide",
        "Ursodeoxycholic Acid", "Udiliv", "Digene", "Gelusil",
        // Vitamins & supplements
        "Vitamin D3", "Cholecalciferol", "Calcium Carbonate", "Shelcal", "Calcium + Vitamin D3", "Vitamin B12",
        "Methylcobalamin", "Neurobion Forte", "Becosules", "Folic Acid", "Iron", "Ferrous Ascorbate", "Livogen",
        "Multivitamin", "Zinc", "Omega 3", "Vitamin C", "Vitamin E", "Magnesium", "Potassium Chloride",
        // Lungs & allergy
        "Montelukast", "Levocetirizine", "Cetirizine", "Fexofenadine", "Salbutamol", "Budesonide", "Formoterol",
        "Tiotropium", "Theophylline", "Doxofylline", "Asthalin Inhaler", "Foracort Inhaler", "Seroflo Inhaler",
        "Montair LC",
        // Brain, nerves, sleep, mood
        "Donepezil", "Memantine", "Rivastigmine", "Levodopa", "Carbidopa Levodopa", "Syndopa", "Pramipexole",
        "Ropinirole", "Trihexyphenidyl", "Gabapentin", "Pregabalin", "Levetiracetam", "Sodium Valproate",
        "Phenytoin", "Carbamazepine", "Clonazepam", "Alprazolam", "Lorazepam", "Zolpidem", "Melatonin",
        "Escitalopram", "Sertraline", "Fluoxetine", "Paroxetine", "Mirtazapine", "Amitriptyline", "Duloxetine",
        "Quetiapine", "Olanzapine", "Risperidone", "Betahistine", "Cinnarizine",
        // Urine & prostate, kidney
        "Tamsulosin", "Silodosin", "Finasteride", "Dutasteride", "Tolterodine", "Solifenacin", "Sodium Bicarbonate",
        "Urimax", "Febuxostat",
        // Antibiotics & infections
        "Amoxicillin", "Amoxicillin Clavulanate", "Augmentin", "Azithromycin", "Cefixime", "Cefuroxime",
        "Ciprofloxacin", "Levofloxacin", "Ofloxacin", "Doxycycline", "Nitrofurantoin", "Metronidazole",
        "Fluconazole", "Acyclovir", "Valacyclovir",
        // Eyes, skin, others
        "Eye Drops", "Timolol Eye Drops", "Latanoprost Eye Drops", "Carboxymethylcellulose Eye Drops",
        "Moisturising Cream", "Calamine Lotion", "Nasal Spray", "Cough Syrup", "ORS",
        // Ayurveda commonly taken
        "Ashwagandha", "Triphala", "Chyawanprash", "Giloy", "Arjuna", "Shilajit"
    ).distinct()

    /** Up to [max] suggestions for what has been typed so far; names starting with it come first. */
    fun suggest(typed: String, extra: List<String> = emptyList(), max: Int = 6): List<String> {
        val q = typed.trim().lowercase()
        if (q.length < 2) return emptyList()
        val pool = (extra + common).distinctBy { it.lowercase() }
        val starts = pool.filter { it.lowercase().startsWith(q) }
        val contains = pool.filter { !it.lowercase().startsWith(q) && it.lowercase().contains(q) }
        return (starts + contains).filter { it.lowercase() != q }.take(max)
    }
}
