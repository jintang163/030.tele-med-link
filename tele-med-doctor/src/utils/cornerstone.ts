import * as cornerstone from 'cornerstone-core'
import * as cornerstoneMath from 'cornerstone-math'
import * as cornerstoneTools from 'cornerstone-tools'
import cornerstoneWADOImageLoader from 'cornerstone-wado-image-loader'
import dicomParser from 'dicom-parser'

let initialized = false

export function initCornerstone() {
  if (initialized) return

  cornerstoneWADOImageLoader.external.cornerstone = cornerstone
  cornerstoneWADOImageLoader.external.dicomParser = dicomParser

  cornerstoneWADOImageLoader.configure({
    useWebWorkers: true,
    webWorkerPath: '',
    taskConfiguration: {
      decodeTask: {
        codecsPath: ''
      }
    }
  })

  cornerstoneTools.external.cornerstone = cornerstone
  cornerstoneTools.external.cornerstoneMath = cornerstoneMath

  const WwwcTool = cornerstoneTools.WwwcTool
  const PanTool = cornerstoneTools.PanTool
  const ZoomTool = cornerstoneTools.ZoomTool
  const StackScrollMouseWheelTool = cornerstoneTools.StackScrollMouseWheelTool
  const LengthTool = cornerstoneTools.LengthTool
  const RectangleRoiTool = cornerstoneTools.RectangleRoiTool
  const EllipticalRoiTool = cornerstoneTools.EllipticalRoiTool
  const AngleTool = cornerstoneTools.AngleTool
  const ArrowAnnotateTool = cornerstoneTools.ArrowAnnotateTool

  cornerstoneTools.addTool(WwwcTool)
  cornerstoneTools.addTool(PanTool)
  cornerstoneTools.addTool(ZoomTool)
  cornerstoneTools.addTool(StackScrollMouseWheelTool)
  cornerstoneTools.addTool(LengthTool)
  cornerstoneTools.addTool(RectangleRoiTool)
  cornerstoneTools.addTool(EllipticalRoiTool)
  cornerstoneTools.addTool(AngleTool)
  cornerstoneTools.addTool(ArrowAnnotateTool)

  const MouseWheelInput = cornerstoneTools.MouseWheelInput
  cornerstoneTools.addInput(MouseWheelInput, {})
  cornerstoneTools.addStackScrollTool('StackScrollMouseWheel')

  initialized = true
  console.log('Cornerstone initialized')
}

export function loadDicomImage(url: string, element: HTMLElement) {
  const wadouri = 'wadouri:' + url
  return cornerstone.loadImage(wadouri).then((image) => {
    cornerstone.displayImage(element, image)
    cornerstoneTools.reset(element)
    return image
  })
}

export function enableElement(element: HTMLElement) {
  cornerstone.enable(element)
}

export function disableElement(element: HTMLElement) {
  try {
    cornerstone.disable(element)
  } catch (e) {
    // ignore
  }
}

export function setToolActive(element: HTMLElement, toolName: string, options?: any) {
  const toolMap: Record<string, string> = {
    wwwc: 'Wwwc',
    pan: 'Pan',
    zoom: 'Zoom',
    length: 'Length',
    rectangle: 'RectangleRoi',
    ellipse: 'EllipticalRoi',
    angle: 'Angle',
    annotate: 'ArrowAnnotate'
  }
  const actualTool = toolMap[toolName] || toolName
  cornerstoneTools.setToolActiveForElement(element, actualTool, options || {})
}

export function setToolDisabled(element: HTMLElement, toolName: string) {
  cornerstoneTools.setToolDisabledForElement(element, toolName)
}

export function getViewport(element: HTMLElement) {
  return cornerstone.getViewport(element)
}

export function setViewport(element: HTMLElement, viewport: any) {
  cornerstone.setViewport(element, viewport)
  cornerstone.updateImage(element)
}

export function resetViewport(element: HTMLElement) {
  cornerstone.reset(element)
}

export function resize(element: HTMLElement) {
  cornerstone.resize(element, true)
}

export type DicomTool = 'wwwc' | 'pan' | 'zoom' | 'length' | 'rectangle' | 'ellipse' | 'angle' | 'annotate' | 'none'
