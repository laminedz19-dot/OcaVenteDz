package com.example.data.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.ui.graphics.vector.ImageVector

data class MarketplaceCategory(
    val id: String,
    val nameAr: String,
    val nameFr: String,
    val subcategoriesAr: List<String>,
    val subcategoriesFr: List<String>,
    val iconName: String
)

object CategoriesData {
    val allCategories = listOf(
        MarketplaceCategory(
            id = "electronics",
            nameAr = "الهواتف والإلكترونيات",
            nameFr = "Téléphones & Électronique",
            subcategoriesAr = listOf("هواتف ذكية", "أجهزة لوحية", "حواسيب ولابتوب", "شاشات وتلفزيونات", "إكسسوارات وسماعات", "كاميرات"),
            subcategoriesFr = listOf("Smartphones", "Tablettes", "PC & Laptops", "TV & Écrans", "Accessoires", "Caméras"),
            iconName = "phone"
        ),
        MarketplaceCategory(
            id = "vehicles",
            nameAr = "السيارات والدراجات",
            nameFr = "Véhicules & Motos",
            subcategoriesAr = listOf("سيارات سياحية", "مركبات نفعية", "دراجات نارية", "شاحنات وحافلات", "مقطورات"),
            subcategoriesFr = listOf("Voitures", "Véhicules Utilitaires", "Motos", "Camions & Bus", "Remorques"),
            iconName = "car"
        ),
        MarketplaceCategory(
            id = "real_estate",
            nameAr = "العقارات",
            nameFr = "Immobilier",
            subcategoriesAr = listOf("شقق للبيع", "شقق للكراء", "منازل وفلل", "أراضي ومزارع", "محلات تجارية", "مكاتب"),
            subcategoriesFr = listOf("Appartements Vente", "Appartements Location", "Maisons & Villas", "Terrains", "Locaux", "Bureaux"),
            iconName = "home"
        ),
        MarketplaceCategory(
            id = "furniture",
            nameAr = "الأثاث والأجهزة المنزلية",
            nameFr = "Maison & Électroménager",
            subcategoriesAr = listOf("أثاث صالون ومجالس", "غرف نوم وأسرة", "ثلاجات ومجمدات", "غسالات", "أفران ومطابخ", "ديكورات"),
            subcategoriesFr = listOf("Salons", "Chambres à coucher", "Réfrigérateurs", "Lave-linges", "Cuisinières", "Décoration"),
            iconName = "weekend"
        ),
        MarketplaceCategory(
            id = "fashion",
            nameAr = "الملابس والأحذية",
            nameFr = "Mode & Vêtements",
            subcategoriesAr = listOf("ملابس رجالية", "ملابس نسائية", "ملابس أطفال ورضع", "أحذية رجالية ونسائية", "حقائب ومحافظ", "ساعات ومجوهرات"),
            subcategoriesFr = listOf("Mode Homme", "Mode Femme", "Enfants & Bébés", "Chaussures", "Sacs", "Montres"),
            iconName = "checkroom"
        ),
        MarketplaceCategory(
            id = "tools",
            nameAr = "أدوات العمل والمعدات",
            nameFr = "Outils & Équipements",
            subcategoriesAr = listOf("أدوات بناء وترميم", "معدات صناعية", "أدوات كهربائية", "أدوات يدوية", "معدات فلاحية"),
            subcategoriesFr = listOf("Outillage BTP", "Matériel Industriel", "Électroportatif", "Outils Manuels", "Matériel Agricole"),
            iconName = "build"
        ),
        MarketplaceCategory(
            id = "games",
            nameAr = "الألعاب وأجهزة الفيديو",
            nameFr = "Jeux Vidéo & Consoles",
            subcategoriesAr = listOf("PlayStation", "Xbox", "Nintendo", "ألعاب الكمبيوتر PC", "إكسسوارات كونسول", "ألعاب أطفال"),
            subcategoriesFr = listOf("PlayStation", "Xbox", "Nintendo", "Jeux PC", "Accessoires Console", "Jeux Enfants"),
            iconName = "sports_esports"
        ),
        MarketplaceCategory(
            id = "spare_parts",
            nameAr = "قطع الغيار واللوازم",
            nameFr = "Pièces Détachées",
            subcategoriesAr = listOf("قطع غيار سيارات", "إطارات وعجلات", "بطاريات", "زيوت وفلاتر", "قطع غيار دراجات"),
            subcategoriesFr = listOf("Pièces Auto", "Pneus & Jantes", "Batteries", "Huiles & Filtres", "Pièces Moto"),
            iconName = "settings"
        ),
        MarketplaceCategory(
            id = "books",
            nameAr = "الكتب والمراجع",
            nameFr = "Livres & Scolaire",
            subcategoriesAr = listOf("كتب مدرسية وبكالوريا", "مراجع جامعية", "روايات وأدب", "كتب دينية وتاريخ", "مستلزمات مكتبية"),
            subcategoriesFr = listOf("Livres Scolaires", "Universitaire", "Romans", "Religieux & Histoire", "Fournitures"),
            iconName = "book"
        ),
        MarketplaceCategory(
            id = "animals",
            nameAr = "الحيوانات المسموحة",
            nameFr = "Animaux & Accessoires",
            subcategoriesAr = listOf("طيور وأسماك", "قطط أليفة", "أغنام ومواشي", "أعلاف ومستلزمات تربية"),
            subcategoriesFr = listOf("Oiseaux & Poissons", "Chats", "Bétail", "Aliments & Soins"),
            iconName = "pets"
        ),
        MarketplaceCategory(
            id = "other",
            nameAr = "فئات أخرى",
            nameFr = "Autres Catégories",
            subcategoriesAr = listOf("خدمات حرفية", "شحن ونقل", "حرف يدوية", "متفرقات"),
            subcategoriesFr = listOf("Services", "Transport", "Artisanat", "Divers"),
            iconName = "auto_awesome"
        )
    )

    fun getCategoryIcon(iconName: String): ImageVector {
        return when (iconName) {
            "phone" -> Icons.Default.PhoneAndroid
            "car" -> Icons.Default.DirectionsCar
            "home" -> Icons.Default.Home
            "weekend" -> Icons.Default.Weekend
            "checkroom" -> Icons.Default.Checkroom
            "build" -> Icons.Default.Build
            "sports_esports" -> Icons.Default.SportsEsports
            "settings" -> Icons.Default.Settings
            "book" -> Icons.Default.Book
            "pets" -> Icons.Default.Pets
            else -> Icons.Default.AutoAwesome
        }
    }
}
