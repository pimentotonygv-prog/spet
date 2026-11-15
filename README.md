# spet — aplicativo step para Android

Aplicativo de caminhada em português para Android 10 ou superior. O identificador `br.com.passo`, a assinatura local de desenvolvimento e os nomes dos arquivos internos foram mantidos para permitir atualizar a instalação anterior sem perder o histórico.

## Novidades

- Tema claro ou escuro, salvo nas configurações.
- Perfis azul, verde, roxo e laranja, em ambos os temas. Os botões têm contraste calculado de pelo menos 6,38:1 entre texto e fundo.
- Ao abrir o mapa da caminhada atual, o app solicita localização precisa e inicia uma busca por GPS, com apoio de rede quando disponível.
- Marcador atualizado e círculo com raio igual à margem de precisão informada pelo Android, em metros. É uma estimativa de incerteza, não uma garantia de precisão absoluta.
- O botão Iniciar permanece habilitado na tela principal e no mapa. O cronômetro começa sem aguardar uma posição GPS, inclusive com GPS desligado ou sinal fraco. O percurso só recebe pontos confiáveis após o sinal ficar disponível, sem reconstruir o trecho anterior. O Android ainda exige autorização de localização para o serviço que registra o percurso; essa permissão é solicitada se necessário.
- Estados de busca, permissão negada, GPS desligado e localização indisponível, com opção de tentar novamente e acesso às configurações quando necessário.
- Botão Atualizar posição para buscar novamente e centralizar o mapa. Durante a caminhada, usa as atualizações contínuas do serviço de GPS.
- Ícone fornecido pelo usuário, preservado por inteiro com margem na área segura do ícone adaptativo. A imagem original tem apenas 64 × 64 pixels; não foi redesenhada nem recortada.

## Instalação

[Baixe o APK na versão 1.1](https://github.com/pimentotonygv-prog/spet/releases/tag/v1.1), abra `step.apk` no celular e autorize a instalação pelo aplicativo que abriu o arquivo. Para preservar o histórico, instale como atualização da versão anterior. A versão tem código 2 e nome 1.1. É assinada com a mesma chave de desenvolvimento usada no primeiro APK do projeto; não é uma publicação na Play Store.

Na engrenagem, escolha o tema e a cor e toque em Salvar. A interface é recriada para aplicar a aparência; a caminhada em andamento continua no serviço de localização.

## Recursos mantidos

Cronômetro com pausa, distância, estimativa de calorias pelo peso e ritmo médio, equivalência energética em peso com aviso de que não é perda real medida, mapa multitoque, registro com tela bloqueada, histórico, acesso a música e imagem compartilhável/salvável na galeria.

Energia líquida aproximada em terreno plano: `0,5 × peso (kg) × distância (km)`. É equivalente a calcular pela velocidade média e integrar pelo tempo ativo. Não adiciona calorias de repouso quando parado. Equivalente energético: `kcal / 7,7` gramas. Não é uma medição clínica.

## GPS e privacidade

Antes de iniciar, o mapa solicita atualizações GPS a cada segundo e rede a cada dois segundos. Essas solicitações param quando a tela do app deixa de estar visível. Durante o registro, o serviço com notificação solicita GPS a cada dois segundos e continua independente da tela.

O registro descarta posições com margem superior a 25 m, antigas, simuladas ou com velocidade incompatível com caminhada. Pequenos deslocamentos são filtrados conforme a incerteza. Pausas e lacunas de sinal abrem novos segmentos. Não há encaixe artificial nas ruas nem reconstrução de trechos ausentes. A precisão e o consumo de bateria variam com aparelho, ambiente e fabricante.

O histórico permanece em armazenamento privado no celular, com backup desativado. Não há conta, anúncios ou envio de coordenadas a um servidor próprio. Os mapas OpenStreetMap precisam de internet e o provedor recebe seu IP e as áreas consultadas. A biblioteca osmdroid mantém um cache local. Imagens exportadas revelam o percurso e ficam fora do histórico privado.

Ao compartilhar ou salvar, o resumo captura a área visível do mapa; ajuste o zoom e aguarde o carregamento dos blocos. A gravação de imagens próprias via MediaStore no Android 10+ dispensa acesso amplo à galeria.

## Compilação e verificação

Abra esta pasta no Android Studio com SDK 35, Gradle 8.11.1 e JDK 17. Use `gradle assembleDebug lintDebug` com uma distribuição Gradle 8.11.1 instalada. Não há Gradle Wrapper incluído.

Neste ambiente Windows, a compilação utiliza Eclipse ECJ 3.40.0 como alternativa ao javac, por incompatibilidade da resolução nativa de caminhos no ambiente restrito. A assinatura usa a chave de desenvolvimento preservada localmente; ela não está incluída no ZIP do código-fonte.

Foram executados 11 casos de verificação da classificação de posição confiável, incluindo falta de permissão, GPS desligado, precisão insuficiente, posição antiga, futura e inválida. Essa classificação controla a orientação sobre sinal e a tentativa de nova busca; não bloqueia o botão Iniciar. Também foram calculados os contrastes dos oito pares tema/cor e a margem geométrica do ícone. A compilação, o lint, a assinatura e o alinhamento do APK são verificados na entrega.

Não foram realizados testes em celular ou emulador. Ainda é necessário validar: permissões negadas, alternância dos oito visuais, persistência ao reabrir, localização em área aberta e coberta, atualização em movimento, registro com tela bloqueada, consumo de bateria, atualização sobre o APK anterior e exportação para outros aplicativos.
