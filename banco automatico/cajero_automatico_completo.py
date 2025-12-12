import json
import os
import time

try:
    import getpass
    def safe_getpass(prompt=''):
        try:
            return getpass.getpass(prompt + ' ')
        except Exception:
            return input(prompt + ' ')
except Exception:
    def safe_getpass(prompt=''):
        return input(prompt + ' ')

# Clase de Cuenta
class Cuenta:
    def __init__(self, numero, pin, saldo=0):
        self.numero = numero
        self.pin = pin
        self.saldo = saldo

    def depositar(self, monto):
        self.saldo += monto

    def retirar(self, monto):
        if monto <= self.saldo:
            self.saldo -= monto
            return True
        else:
            return False

# Cajero Automático
class CajeroAutomatico:
    def __init__(self):
        self.cuentas = {}
        self.cargar_cuentas()

    def cargar_cuentas(self):
        if os.path.exists('accounts.json'):
            with open('accounts.json', 'r') as f:
                datos = json.load(f)
                for numero, info in datos.items():
                    self.cuentas[numero] = Cuenta(numero, info['pin'], info['saldo'])

    def guardar_cuentas(self):
        datos = {num: {'pin': cuenta.pin, 'saldo': cuenta.saldo} for num, cuenta in self.cuentas.items()}
        with open('accounts.json', 'w') as f:
            json.dump(datos, f)

    def crear_cuenta(self):
        numero = input("Ingrese número de cuenta: ")
        pin = safe_getpass("Ingrese PIN: ")
        if numero in self.cuentas:
            print("La cuenta ya existe.")
            return
        self.cuentas[numero] = Cuenta(numero, pin)
        self.guardar_cuentas()
        print("Cuenta creada con éxito.")

    def iniciar_sesion(self):
        numero = input("Número de cuenta: ")
        pin = safe_getpass("PIN: ")
        cuenta = self.cuentas.get(numero)
        if cuenta and cuenta.pin == pin:
            self.menu_cuenta(cuenta)
        else:
            print("Credenciales incorrectas.")

    def menu_cuenta(self, cuenta):
        while True:
            print("\n--- Menú de Cuenta ---")
            print("1. Consultar saldo")
            print("2. Depositar")
            print("3. Retirar")
            print("4. Salir")
            opcion = input("Seleccione: ")
            if opcion == '1':
                print(f"Saldo actual: {cuenta.saldo}")
            elif opcion == '2':
                monto = float(input("Monto a depositar: "))
                cuenta.depositar(monto)
                self.guardar_cuentas()
                print("Depósito exitoso.")
            elif opcion == '3':
                monto = float(input("Monto a retirar: "))
                if cuenta.retirar(monto):
                    self.guardar_cuentas()
                    print("Retiro exitoso.")
                else:
                    print("Fondos insuficientes.")
            elif opcion == '4':
                break
            else:
                print("Opción inválida.")

    def menu_principal(self):
        while True:
            print("\n--- Cajero Automático ---")
            print("1. Iniciar sesión")
            print("2. Crear cuenta")
            print("3. Salir")
            opcion = input("Seleccione: ")
            if opcion == '1':
                self.iniciar_sesion()
            elif opcion == '2':
                self.crear_cuenta()
            elif opcion == '3':
                print("Gracias por usar el cajero.")
                break
            else:
                print("Opción inválida.")

if __name__ == "__main__":
    cajero = CajeroAutomatico()
    cajero.menu_principal()
