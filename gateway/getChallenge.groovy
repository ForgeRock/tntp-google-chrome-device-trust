import static org.forgerock.util.promise.Promises.*

def challenge

def amRequest = new Request()
amRequest.method = "POST"
amRequest.uri = "https://verifiedaccess.googleapis.com/v2/challenge:generate?key=<GOOGLE-API-KEY>"
return http.send(amRequest).then { response ->
      challenge = response.entity.json.challenge
      logger.info(challenge)
      return response
}
.thenAsync {
      next.handle(context, request).thenOnResult { response ->
             response.headers['x-verified-access-challenge']="{\"challenge\":\"" + challenge + "\"}"
           }
}
