package com.aplicaion.minimarketapp.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aplicaion.minimarketapp.db.dao.CategoriaDao
import com.aplicaion.minimarketapp.db.dao.DetalleVentaDao
import com.aplicaion.minimarketapp.db.dao.ProductoDao
import com.aplicaion.minimarketapp.db.dao.ProveedorDao
import com.aplicaion.minimarketapp.db.dao.UsuarioDao
import com.aplicaion.minimarketapp.db.dao.VentaDao
import com.aplicaion.minimarketapp.db.entity.Categoria
import com.aplicaion.minimarketapp.db.entity.DetalleVenta
import com.aplicaion.minimarketapp.db.entity.Producto
import com.aplicaion.minimarketapp.db.entity.Proveedor
import com.aplicaion.minimarketapp.db.entity.Usuario
import com.aplicaion.minimarketapp.db.entity.Venta
import com.aplicaion.minimarketapp.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Usuario::class,
        Categoria::class,
        Producto::class,
        Proveedor::class,
        Venta::class,
        DetalleVenta::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun productoDao(): ProductoDao
    abstract fun proveedorDao(): ProveedorDao
    abstract fun ventaDao(): VentaDao
    abstract fun detalleVentaDao(): DetalleVentaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .addCallback(DatabaseCallback(context.applicationContext))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(getInstance(context))
                }
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            val usuarioDao = db.usuarioDao()
            val categoriaDao = db.categoriaDao()
            val productoDao = db.productoDao()
            val proveedorDao = db.proveedorDao()

            // 1. Insert default admin user if empty
            if (usuarioDao.getCount() == 0) {
                usuarioDao.insert(
                    Usuario(
                        nombreCompleto = "Administrador Principal",
                        usuario = "admin",
                        correo = "admin@minimarket.com",
                        contrasena = "admin123",
                        rol = Constants.ROL_ADMIN,
                        estado = Constants.ESTADO_ACTIVO
                    )
                )
            }

            // 2. Insert initial categories if empty
            if (categoriaDao.getCount() == 0) {
                val cat1Id = categoriaDao.insert(Categoria(nombre = "Abarrotes", descripcion = "Productos de primera necesidad"))
                val cat2Id = categoriaDao.insert(Categoria(nombre = "Lácteos", descripcion = "Lácteos y derivados"))
                val cat3Id = categoriaDao.insert(Categoria(nombre = "Bebidas", descripcion = "Bebidas y refrescos"))
                val cat4Id = categoriaDao.insert(Categoria(nombre = "Limpieza", descripcion = "Artículos de limpieza del hogar"))
                val cat5Id = categoriaDao.insert(Categoria(nombre = "Snacks", descripcion = "Golosinas y piqueos"))

                // 3. Insert initial supplier
                val provId = proveedorDao.insert(
                    Proveedor(
                        nombre = "Distribuidora AJE",
                        celular = "987654321",
                        direccion = "Av. Central 123",
                        correo = "ventas@aje.com",
                        ruc = "20100123456"
                    )
                )

                // 4. Insert initial products if empty
                if (productoDao.getCount() == 0) {
                    val catBebidasId = if (cat3Id > 0) cat3Id.toInt() else 3
                    val catAbarrotesId = if (cat1Id > 0) cat1Id.toInt() else 1
                    val catLacteosId = if (cat2Id > 0) cat2Id.toInt() else 2

                    productoDao.insert(
                        Producto(
                            nombre = "Coca Cola 3 Litros",
                            categoriaId = catBebidasId,
                            precioCompra = 8.50,
                            precioVenta = 11.00,
                            stock = 25,
                            descripcion = "Gaseosa Coca Cola botella 3L sin retorno",
                            codigoBarras = "7751234567890",
                            proveedorId = provId.toInt(),
                            imagenPath = "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=400"
                        )
                    )

                    productoDao.insert(
                        Producto(
                            nombre = "Inca Kola 1.5 Litros",
                            categoriaId = catBebidasId,
                            precioCompra = 4.20,
                            precioVenta = 5.50,
                            stock = 30,
                            descripcion = "Gaseosa Inca Kola botella 1.5L",
                            codigoBarras = "7751234567891",
                            proveedorId = provId.toInt(),
                            imagenPath = "https://images.unsplash.com/photo-1581006852262-e4307cf6283a?w=400"
                        )
                    )

                    productoDao.insert(
                        Producto(
                            nombre = "Arroz Extra Costeño 1kg",
                            categoriaId = catAbarrotesId,
                            precioCompra = 3.80,
                            precioVenta = 4.80,
                            stock = 4, // Stock bajo < 5 para probar alerta roja
                            descripcion = "Arroz de grano largo y seleccionado",
                            codigoBarras = "7751234567892",
                            proveedorId = provId.toInt(),
                            imagenPath = "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=400"
                        )
                    )

                    productoDao.insert(
                        Producto(
                            nombre = "Leche Evaporada Gloria 400g",
                            categoriaId = catLacteosId,
                            precioCompra = 3.20,
                            precioVenta = 4.00,
                            stock = 50,
                            descripcion = "Leche entera en lata 400g",
                            codigoBarras = "7751234567893",
                            proveedorId = provId.toInt(),
                            imagenPath = "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400"
                        )
                    )
                }
            }
        }
    }
}
