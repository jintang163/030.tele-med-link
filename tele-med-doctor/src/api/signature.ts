import request from '@/utils/request'

export interface Sm2KeyPair {
  publicKey: string
  privateKey: string
}

export interface ConsultationSignatureVO {
  id: number
  consultationId: number
  doctorId: number
  doctorName?: string
  doctorTitle?: string
  department?: string
  signOrder: number
  signStatus: number
  signStatusText: string
  signatureImageUrl?: string
  sm2PublicKey?: string
  signPositionX?: number
  signPositionY?: number
  signWidth?: number
  signHeight?: number
  signPage?: number
  signReason?: string
  signLocation?: string
  signTime?: string
  createTime: string
}

export interface DoctorSignParams {
  consultationId: number
  doctorId: number
  signatureData: string
  signPositionX?: number
  signPositionY?: number
  signWidth?: number
  signHeight?: number
  signPage?: number
  signReason?: string
  signLocation?: string
  sm2PublicKey?: string
  sm2PrivateKey?: string
  sm2Signature?: string
  pdfBase64?: string
}

export function generateSm2KeyPair() {
  return request.post<never, { data: Sm2KeyPair }>('/signature/generate-key-pair')
}

export function sm2SignData(data: string, privateKey: string) {
  return request.post<never, { data: { signature: string } }>('/signature/sign-data', { data, privateKey })
}

export function sm2VerifyData(data: string, publicKey: string, signature: string) {
  return request.post<never, { data: { valid: boolean } }>('/signature/verify-data', { data, publicKey, signature })
}

export function sm3Hash(data: string) {
  return request.post<never, { data: { hash: string } }>('/signature/sm3-hash', { data })
}

export function generateDraftPdf(consultationId: number, conclusionContent: string, imageUrls?: string[]) {
  return request.post<never, { data: { pdfBase64: string } }>('/signature/draft-pdf', {
    consultationId,
    conclusionContent,
    imageUrls
  })
}

export function doctorSign(params: DoctorSignParams) {
  return request.post<never, { data: ConsultationSignatureVO }>('/signature/doctor-sign', params)
}

export function getConsultationSignatures(consultationId: number) {
  return request.get<never, { data: ConsultationSignatureVO[] }>(`/signature/consultation/${consultationId}`)
}

export function getCurrentSigner(consultationId: number) {
  return request.get<never, { data: ConsultationSignatureVO }>(`/signature/current-signer/${consultationId}`)
}

export function checkAllSigned(consultationId: number) {
  return request.get<never, { data: { allSigned: boolean } }>(`/signature/all-signed/${consultationId}`)
}

export function getFinalPdfUrl(consultationId: number) {
  return request.get<never, { data: { url: string } }>(`/signature/pdf-url/${consultationId}`)
}
