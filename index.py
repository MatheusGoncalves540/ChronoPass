import face_recognition
import cv2

# 1. Carregar a foto do funcionário cadastrado
foto_cadastro = face_recognition.load_image_file("funcionario_joao.jpg")
encoding_cadastro = face_recognition.face_encodings(foto_cadastro)[0]

# 2. Capturar a foto no momento do ponto
camera = cv2.VideoCapture(0)
print("Olhe para a câmera para bater o ponto...")
ret, frame = camera.read()
camera.release()

if ret:
    # Encontrar rostos na imagem da câmera
    rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    encodings_camera = face_recognition.face_encodings(rgb_frame)

    if len(encodings_camera) > 0:
        # 3. Comparar o rosto da câmera com o cadastro
        resultado = face_recognition.compare_faces([encoding_cadastro], encodings_camera[0])
        
        if resultado[0]:
            print("Ponto registrado com sucesso! Usuário: João.")
        else:
            print("Erro: Rosto não reconhecido.")
    else:
        print("Nenhum rosto detectado na câmera.")
