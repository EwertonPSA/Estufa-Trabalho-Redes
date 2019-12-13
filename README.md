# Estufa-Trabalho-Redes
Nesta aplicação, as condições da estufa são conﬁguradas, monitoradas e controladas por um gerenciador, que se comunica com os sensores/atuadores dentro da estufa e pode receber conﬁgurações ou responder consultas de um cliente externo. No arquivo Proposta_Trabalho_Redes.pdf é apresentado mais detalhes sobre o escopo do projeto e seus requisitos.

Este trabalho foi desenvolvido em duas etapas: Estabelecer protocolo de comunicação entre os equipamentos e desenvolvimento da da aplicação. No arquivo protocolo_Comunicação_Equipamento.pdf é apresentado o protocolo de comunicação usado na rede de comunicação entre os equipamentos.

Nossa aplicação foi implementada na linguagem Java 8 com a IDE Eclipse no sistema operacional Windows 10. Seu funcionamento ocorre de acordo com o protocolo estabelecido na primeira etapa do trabalho.

### Execução
Os processos de cada dispositivo podem ser executados buscando o diretório /Simulador no diretório principal da aplicação pelo Prompt de Comando do Windows e inserindo              o comando: 
 
java -jar <nome do executável>.jar 

Obs: O processo do Gerenciador deve ser o primeiro a ser executado. 

### Observações 
1 - Há 9 processos a serem executados: 
1. Cliente 
2. Gerenciador 
3. SensorTemperatura 
4. SensorCO2 
5. SensorUmidade 
6. Aquecedor 
7. Resfriador 
8. Injetor 
9. Irrigador  
 
2 - As mensagens de leitura enviadas pelos sensores ao gerenciador não são explicitadas no gerenciador por texto, visto que isso acarretaria em um spam,            impossibilitando a visualização das outras mensagens. Ainda é possível verificar essa troca através da mensagem 7 (Requisição da última             leitura dos sensores) do cliente para o gerenciador, notando-se que os valores são             alterados e correspondem aos valores impressos nos processos dos sensores. 
 
3 - Para simular a contribuição da temperatura do ambiente na temperatura da estufa, o usuário pode abrir o arquivo “temperaturaAmbiente.txt” e alterar seu valor            
manualmente. De acordo com a temperatura ambiente, a temperatura lida no sensor de temperatura sobe ou desce automaticamente. Assim, é possível testar o funcionamento tanto do Aquecedor quanto do Resfriador. 
